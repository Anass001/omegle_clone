package com.zeneo.omechle.ui.main.text;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.zeneo.omechle.R;
import com.zeneo.omechle.adapter.MessagesListAdapter;
import com.zeneo.omechle.constant.State;
import com.zeneo.omechle.databinding.FragmentTextBinding;
import com.zeneo.omechle.model.Message;
import com.zeneo.omechle.model.Room;
import com.zeneo.omechle.network.stomp.Stomp;
import com.zeneo.omechle.repository.MatchingRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TextFragment extends Fragment {

    private final static String TAG = "Text Fragment";

    private String userId;
    private ObservableField<State> state = new ObservableField<>();
    private Room currentRoom;
    private List<Message> messages = new ArrayList<>();

    private MatchingRepository matchingRepository;

    private EditText messageEditText;

    private MessagesListAdapter adapter;

    private ObservableInt count = new ObservableInt(0);

    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentTextBinding textBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_text, container, false);
        textBinding.setState(state);
        textBinding.setCount(count);
        return textBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // init views
        messageEditText = view.findViewById(R.id.message_input);
        ImageButton sendButton = view.findViewById(R.id.send_button);
        ImageButton stopButton = view.findViewById(R.id.stop_button);
        ImageButton nextButton = view.findViewById(R.id.next_button);
        sendButton.setOnClickListener(v -> {
            sendMessage();
        });
        stopButton.setOnClickListener(v -> {
            leftRoom();
        });
        nextButton.setOnClickListener(v -> {
            next();
        });

        RecyclerView messagesList = view.findViewById(R.id.messages_list);
        adapter = new MessagesListAdapter(messages, getContext());
        messagesList.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesList.setAdapter(adapter);

        state.set(State.CONNECTING);
        matchingRepository = new MatchingRepository();
        matchingRepository.connect((status, frame) -> {
            if (status == Stomp.CONNECTED) {
                Log.i(TAG, "connection opened");
                Log.i(TAG, "headers: " + frame.getHeaders());
                userId = frame.getHeaders().get("user-name");
                start();
            }
        });

    }

    @SuppressLint("CheckResult")
    private void start() {
        state.set(State.IN_QUEUE);
        matchingRepository.startMatching("text");
        matchingRepository.watchMatching(userId, (headers, body) -> {
            matchingRepository.acceptQueue(userId, body);
        });
        matchingRepository.watchAcceptedMatching(userId, (header, body) -> {
            state.set(State.IN_ROOM);
            currentRoom = new Gson().fromJson(body, Room.class);
            watchRoom();
        });
    }

    private void watchRoom() {
        if (state.get() == State.IN_ROOM) {
            firebaseFirestore.collection("rooms").document(currentRoom.getId()).addSnapshotListener((value, error) -> {
                if (value != null && value.exists()) {
                    Log.d(TAG, "Current data: " + value.getData());
                    Message message = Message.fromMap((Map<String, Object>) value.getData().get("message"));
                    if (message.getFrom().equals(userId))
                        message.setMe(true);
                    else
                        message.setMe(false);
                    messages.add(message);
                    count.set(count.get() + 1);
                    Log.d(TAG, "Current data: " + messages.toString());
                    adapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "Current data: null");
                }
            });
            matchingRepository.watchRoom(currentRoom.getId(), (headers, body) -> {
                Map<String, Objects> data = new Gson().fromJson(body, Map.class);
                if (data.get("type").equals("exit")) {
                    leftRoom();
                }
            });
        }
    }

    private void sendMessage() {
        if (state.get() == State.IN_ROOM) {
            Message message = new Message();
            message.setFrom(userId);
            message.setSentAt(new Date(System.currentTimeMillis()));
            message.setRoomId(currentRoom.getId());
            message.setText(messageEditText.getText().toString().trim());
            if (!message.getText().equals("")) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", "message");
                map.put("message", message);
                firebaseFirestore.collection("rooms").document(currentRoom.getId()).set(map);
                messageEditText.setText("");
            }
        }
    }

    public void leftRoom() {
        if (state.get() == State.IN_ROOM) {
            matchingRepository.sendExit(currentRoom.getId());
            state.set(State.LEFT);
        }
    }

    public void next() {
        if (state.get() == State.LEFT) {
            start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        matchingRepository.disconnect();
    }
}