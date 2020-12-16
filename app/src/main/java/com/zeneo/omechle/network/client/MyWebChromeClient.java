package com.zeneo.omechle.network.client;

import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;

public class MyWebChromeClient extends WebChromeClient {


    @Override
    public void onPermissionRequest(PermissionRequest request) {
        request.grant(request.getResources());
    }
}
