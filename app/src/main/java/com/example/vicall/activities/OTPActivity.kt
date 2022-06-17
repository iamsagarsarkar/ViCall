package com.example.vicall.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.vicall.R
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_otpactivity.*

class OTPActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var verificationId : String
    private lateinit var dbFireStore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otpactivity)

        firebaseAuth = Firebase.auth
        dbFireStore = Firebase.firestore
        verificationId = intent.getStringExtra("verificationId")!!

        btn_otp_verification.setOnClickListener {
            if (et_otp.text.toString().isNotEmpty()){
                val credential = PhoneAuthProvider.getCredential(verificationId,et_otp.text.toString())
                signInWithPhoneAuthCredential(credential)
            }else{
                Toast.makeText(this,"Enter Phone Number", Toast.LENGTH_SHORT).show()

            }
        }

    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    getUserData(user!!)
                } else {
                    Log.w("Phone Verification", "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(this,"Invalid OTP", Toast.LENGTH_SHORT).show()

                    }
                }
            }
    }



    // update ui
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

    private fun reloadUploadActivity() {
        val intent = Intent(this, UploadDetailsActivity::class.java)
        startActivity(intent)
        finish()
    }
}