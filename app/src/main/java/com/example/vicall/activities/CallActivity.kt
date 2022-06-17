package com.example.vicall.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.vicall.R
import com.example.vicall.interfaces.JavascriptInterface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_call.*

import com.google.firebase.firestore.FirebaseFirestore

class CallActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore

    private lateinit var userId : String
    private lateinit var anotherUserId : String
    private lateinit var caller : String

    private var isAudio : Boolean = true
    private var isVideo : Boolean = true


    private var isPeerConnected : Boolean = false
    private  var callState : String = ""
    private var condition : String = ""

    private lateinit var timer: CountDownTimer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        auth = Firebase.auth
        dbFireStore = FirebaseFirestore.getInstance()

        userId = intent.getStringExtra("userId")!!
        anotherUserId = intent.getStringExtra("anotherUserId")!!
        caller = intent.getIntExtra("caller",2).toString()


        iv_toggle_audio.setOnClickListener(this)
        iv_toggle_video.setOnClickListener(this)
        iv_end_call.setOnClickListener(this)

        setupWebView()


        timer()

    }



    private fun sendCallRequest() {
        if (!isPeerConnected) {
            Toast.makeText(this, "You're not connected. Check your internet", Toast.LENGTH_LONG).show()
            return
        }
        callState = "callMade"
        condition = "callMade"
        timer.cancel()
        callJavascriptFunction("javascript:startCall(\"${anotherUserId}\")")
    }


    @SuppressLint("SetJavaScriptEnabled")
     fun setupWebView() {

        wv_call.webChromeClient = object: WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        wv_call.settings.javaScriptEnabled = true
        wv_call.settings.mediaPlaybackRequiresUserGesture = false
        wv_call.addJavascriptInterface(JavascriptInterface(this), "Android")

        loadVideoCall()
    }

    private fun loadVideoCall() {
        val filePath = "file:android_asset/index.html"
        wv_call.loadUrl(filePath)

        wv_call.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                initializePeer()
            }
        }
    }



    private fun initializePeer() {

        callJavascriptFunction("javascript:init(\"${userId}\")")

        if (caller == "1"){
            condition = "reject"
            callState = "callMissedOutgoing"
            val document =  dbFireStore.collection("userCalls").document(auth.uid!!)
            document.addSnapshotListener{snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    if (snapshot.data?.get(auth.uid) == anotherUserId){
                       sendCallRequest()
                    }
                }}
        }else{
            condition = "received"
            callState = "callReceived"
        }
        dbFireStore.collection("userCalls").document(anotherUserId).addSnapshotListener{snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                if (snapshot.data?.get(anotherUserId) == "1"){
                    setUserCalls()
                }
            }}
    }


    private fun callJavascriptFunction(functionString: String) {
        wv_call.post { wv_call.evaluateJavascript(functionString, null) }
    }


    fun onPeerConnected() {
        isPeerConnected = true
    }

    override fun onBackPressed() {
        setUserCalls()
    }

    override fun onDestroy() {
        wv_call.loadUrl("about:blank")
        super.onDestroy()
    }

    private fun setUserCalls() {
        val userValue = hashMapOf(
            auth.uid to "1",
        )
        val anotherUserValue = hashMapOf(
            anotherUserId to "1",
        )
        dbFireStore.collection("userCalls").document(auth.uid!!).set(userValue).addOnCompleteListener {
            dbFireStore.collection("userCalls").document(anotherUserId).set(anotherUserValue).addOnCompleteListener {
                val data  = Intent()
                data.putExtra("condition",condition)
                data.putExtra("callState",callState)
                setResult(RESULT_OK,data)
                finish()
            }
        }
    }

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.iv_toggle_audio ->{
                isAudio = !isAudio
                callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
                iv_toggle_audio.setImageResource(if (isAudio) R.drawable.ic_baseline_mic else R.drawable.ic_baseline_mic_off)
            }
            R.id.iv_toggle_video ->{
                isVideo = !isVideo
                callJavascriptFunction("javascript:toggleVideo(\"${isVideo}\")")
                iv_toggle_video.setImageResource(if (isVideo) R.drawable.ic_baseline_videocam else R.drawable.ic_baseline_videocam_off)
            }
            R.id.iv_end_call ->{
                condition = "cancel"
                setUserCalls()
            }
        }
    }

    private fun timer(){
        timer = object : CountDownTimer(30000,1000){
            override fun onTick(p0: Long) {}

            override fun onFinish() {
                condition = "offline"
                setUserCalls()
            }
        }
        timer.start()
    }

    companion object{
        private const val RESULT_OK = 4
    }

}