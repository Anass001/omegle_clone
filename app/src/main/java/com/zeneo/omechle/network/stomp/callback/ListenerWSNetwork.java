package com.zeneo.omechle.network.stomp.callback;

import com.zeneo.omechle.network.stomp.Frame;

import javax.annotation.Nullable;

public interface ListenerWSNetwork {
    public void onState(int state, @Nullable Frame frame);
}
