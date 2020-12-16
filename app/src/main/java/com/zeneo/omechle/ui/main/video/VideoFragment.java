package com.zeneo.omechle.ui.main.video;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.zeneo.omechle.JSCallback;
import com.zeneo.omechle.MainActivity;
import com.zeneo.omechle.R;
import com.zeneo.omechle.adapter.MessagesListAdapter;
import com.zeneo.omechle.constant.State;
import com.zeneo.omechle.databinding.FragmentVideoBinding;
import com.zeneo.omechle.model.Message;
import com.zeneo.omechle.model.Room;
import com.zeneo.omechle.model.User;
import com.zeneo.omechle.network.client.MyWebChromeClient;
import com.zeneo.omechle.network.client.MyWebViewClient;
import com.zeneo.omechle.network.stomp.Stomp;
import com.zeneo.omechle.repository.MatchingRepository;
import com.zeneo.omechle.ui.main.MainFragment;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VideoFragment extends Fragment implements JSCallback {

    private final static String TAG = "Video Fragment";
    private String userId;
    private ObservableField<State> state = new ObservableField<>();
    private Room currentRoom;
    private List<Message> messages = new ArrayList<>();

    private MatchingRepository matchingRepository;

    private EditText messageEditText;

    private WebView webView;

    private MessagesListAdapter adapter;

    private ObservableInt count = new ObservableInt(0);

    private FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermission(158);
    }

    // Function to check and request permission
    public void checkPermission(int requestCode)
    {

        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat
                    .requestPermissions(
                            getActivity(),
                            new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.MODIFY_AUDIO_SETTINGS},
                            requestCode);
        }
        else {
            Log.d(TAG, "Permissions already granted!");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(getActivity(), "Permissions Granted!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, String.valueOf(grantResults.length));
        } else {
            Log.d(TAG, "Permissions Denied!");
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentVideoBinding videoBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_video, container, false);
        videoBinding.setState(state);
        videoBinding.setCount(count);
        return videoBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        RecyclerView messagesList = view.findViewById(R.id.messages_list);
        adapter = new MessagesListAdapter(messages, getContext());
        messagesList.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesList.setAdapter(adapter);

        state.set(State.CONNECTING);
        matchingRepository = new MatchingRepository();
        matchingRepository.connect((status, frame) -> {
            if (status == Stomp.CONNECTED) {
                userId = frame.getHeaders().get("user-name");
                setupWebView();
            }
        });

    }

    void initViews(View view) {
        // init views
        messageEditText = view.findViewById(R.id.message_input);
        ImageButton sendButton = view.findViewById(R.id.send_button);
        ImageButton stopButton = view.findViewById(R.id.stop_button);
        ImageButton nextButton = view.findViewById(R.id.next_button);
        webView = view.findViewById(R.id.call_web_view);
        sendButton.setOnClickListener(v -> {
            sendMessage();
        });
        stopButton.setOnClickListener(v -> {
            leftRoom();
        });
        nextButton.setOnClickListener(v -> {
            next();
        });
    }

    private void setupWebView() {
        webView.post(() -> {
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    Log.d(TAG, "onPermissionRequest");
                    getActivity().runOnUiThread(() -> {
                            request.grant(request.getResources());
                    });
                }
                @Override
                public boolean onConsoleMessage(ConsoleMessage cm) {
                    Log.d("MyApplication", cm.message() + " -- From line "
                            + cm.lineNumber() + " of "
                            + cm.sourceId() );
                    return true;
                }
            });
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
            webView.addJavascriptInterface(this, "Android");
            loadWebView();
        });
    }

    private void loadWebView() {
        webView.loadUrl("file:android_asset/call.html");
        webView.setWebViewClient(new MyWebViewClient(this::initializePeer));
    }

    private void initializePeer() {
        callJavascriptFunction("javascript:init(\""+ userId +"\")");
    }

    private void callJavascriptFunction(String f) {
        webView.post(() -> webView.evaluateJavascript(f, null));
    }

    @SuppressLint("CheckResult")
    private void start() {
        state.set(State.IN_QUEUE);
        matchingRepository.startMatching("video");
        matchingRepository.watchMatching(userId, (headers, body) -> {
            matchingRepository.acceptQueue(userId, body);
        });
        matchingRepository.watchAcceptedMatching(userId, (header, body) -> {
            state.set(State.IN_ROOM);
            currentRoom = new Gson().fromJson(body, Room.class);
            if (currentRoom.getUsers().get(0).getId().equals(userId)) {
                    callJavascriptFunction("javascript:startCall(\""+ currentRoom.getUsers().get(1).getId() +"\")");
            }
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
    @JavascriptInterface
    public void onPeerConnected() {
        start();
        //callJavascriptFunction("javascript:listen()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        matchingRepository.disconnect();
    }

}