package com.example.vicall.interfaces

import android.webkit.JavascriptInterface
import com.example.vicall.activities.CallActivity

class JavascriptInterface(private val callActivity: CallActivity) {

    @JavascriptInterface
    public fun onPeerConnected() {
        callActivity.onPeerConnected()
    }

}