package com.zeneo.omechle.repository;

import com.google.gson.Gson;
import com.zeneo.omechle.network.stomp.Stomp;
import com.zeneo.omechle.network.stomp.Subscription;
import com.zeneo.omechle.network.stomp.callback.ListenerSubscription;
import com.zeneo.omechle.network.stomp.callback.ListenerWSNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MatchingRepository {

    private static final String TAG = "Matching Repository";

    private Stomp mStompClient;

    private List<Subscription> subscriptions = new ArrayList<>();


    public void connect(ListenerWSNetwork listenerWSNetwork) {
        mStompClient = new Stomp("wss://omexle.herokuapp.com/ws", null, listenerWSNetwork);
    }

    public void watchMatching(String id, ListenerSubscription listenerSubscription) {
        Subscription subscription = new Subscription("/topic/matched/" + id, listenerSubscription);
        subscriptions.add(subscription);
        mStompClient.subscribe(subscription);
    }

    public void watchAcceptedMatching(String id, ListenerSubscription listenerSubscription) {
        Subscription subscription = new Subscription("/topic/myRoom/" + id, listenerSubscription);
        subscriptions.add(subscription);
        mStompClient.subscribe(subscription);
    }

    public void watchRoom(String roomId, ListenerSubscription listenerSubscription) {
        Subscription subscription = new Subscription("/topic/room/" + roomId, listenerSubscription);
        subscriptions.add(subscription);
        mStompClient.subscribe(subscription);
    }

    public void acceptQueue(String myId, String strangerId) {
        Map<String, String> map = new HashMap<>();
        map.put("user1", myId);
        map.put("user2", strangerId);

        mStompClient.send("/app/matchingAccepted", null, new Gson().toJson(map));
    }

    public void startMatching(String type) {
        mStompClient.send("/app/matching/" + type, null, null);
    }

    public void sendExit(String roomId) {
        mStompClient.send("/app/myRoom/exit", null, roomId);
        for (Subscription subscription: subscriptions) {
            mStompClient.unsubscribe(subscription.getId());
        }
    }

    public void disconnect() {
        mStompClient.disconnect();
    }
}
