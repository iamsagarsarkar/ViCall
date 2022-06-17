package com.example.vicall.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AnimationUtils
import com.example.vicall.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_splash_screen.*

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)


        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        firebaseAuth = Firebase.auth
        dbFireStore = Firebase.firestore


        iv_splash_screen.animation = AnimationUtils.loadAnimation(this, R.anim.image_animation)
        tv_splash_screen.animation = AnimationUtils.loadAnimation(this, R.anim.text_animation)
    }



    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        Handler().postDelayed(Runnable {
            if(currentUser != null){
                getUserData(currentUser)
            }else{
                reloadLoginActivity()
            }
        },1600)
    }


    private fun getUserData(currentUser: FirebaseUser) {
        dbFireStore.collection("userInfo").document(currentUser.uid).get()
            .addOnSuccessListener { taskSnapshot ->
                if (taskSnapshot.exists()){
                    reloadRecentActivity()
                }else {
                    reloadUploadActivity()
                }
            }
    }



    private fun reloadRecentActivity() {
        val intent = Intent(this, RecentCallActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun reloadLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun reloadUploadActivity() {
        val intent = Intent(this, UploadDetailsActivity::class.java)
        startActivity(intent)
        finish()
    }
}