package com.zeneo.omechle.network.client;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyWebViewClient extends WebViewClient {

    public interface OnFinishedListener {
        void onFinished();
    }

    private OnFinishedListener onFinishedListener;

    public MyWebViewClient(OnFinishedListener onFinishedListener) {
        this.onFinishedListener = onFinishedListener;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        onFinishedListener.onFinished();
    }
}
