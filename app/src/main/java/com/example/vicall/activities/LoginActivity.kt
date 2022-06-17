package com.example.vicall.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.vicall.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var resendingToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var mVerificationId : String

    private var phoneNumber : String? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = Firebase.auth
        dbFireStore = Firebase.firestore



        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(phoneAuthCredential : PhoneAuthCredential) {

                signInWithPhoneAuthCredential(phoneAuthCredential)
                Log.d("phone verification" , "Verification Completed Success")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.d("phone verification" , "Verification Failed",e)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)

                mVerificationId = verificationId
                resendingToken = token

                val intent = Intent(applicationContext, OTPActivity::class.java)
                intent.putExtra("verificationId",verificationId)
                startActivity(intent)
                finish()

            }
    }

        btn_phone_login.setOnClickListener {
            when {
                et_phone_number.text.toString().isEmpty() ->{
                    Toast.makeText(this,"Enter Phone Number",Toast.LENGTH_SHORT).show()
                }
                et_phone_number.text.toString().startsWith("91") ->{
                    Toast.makeText(this,"Enter Number Without Country Code",Toast.LENGTH_SHORT).show()
                }
                et_phone_number.text.toString().length != 10 -> {
                    Toast.makeText(this,"Enter Valid Number",Toast.LENGTH_SHORT).show()
                }
                else ->{
                    logInWithPhoneNumber()
                }
            }

        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if(currentUser != null){
            reloadRecentActivity()
        }
    }


    private fun logInWithPhoneNumber(){

        phoneNumber = "+91${et_phone_number.text.toString()}"
        startPhoneNumberVerification(phoneNumber!!)

    }


    private fun startPhoneNumberVerification(phoneNumber: String) {

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
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