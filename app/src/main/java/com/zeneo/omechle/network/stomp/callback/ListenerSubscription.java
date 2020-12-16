package com.zeneo.omechle.network.stomp.callback;

import java.util.Map;

public interface ListenerSubscription {
    public void onMessage(Map<String, String> headers, String body);
}
