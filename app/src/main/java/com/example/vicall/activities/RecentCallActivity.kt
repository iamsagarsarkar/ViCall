package com.example.vicall.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vicall.R
import com.example.vicall.adapters.ContactAdapter
import com.example.vicall.adapters.RecentCallsAdapter
import com.example.vicall.models.ContactModel
import com.example.vicall.models.MyCalls
import com.example.vicall.models.RecentCallModel
import com.example.vicall.onClick.OnRecentCallClick
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.android.synthetic.main.activity_recent_call.*
import java.lang.reflect.Array.get
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RecentCallActivity : AppCompatActivity(),OnRecentCallClick, View.OnClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore

    private lateinit var adapter: RecentCallsAdapter
    private lateinit var callList : ArrayList<RecentCallModel>

    private var anotherUserUid = ""
    private lateinit var anotherUserName : String
    private lateinit var anotherUserPhoto : String
    private lateinit var anotherUserPhone : String
    private lateinit var currentDate : String

    private lateinit var callState : String
    private var isCaller : Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recent_call)


        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(activity_recent_list_toolBar)



        auth = Firebase.auth
        dbFireStore = FirebaseFirestore.getInstance()

        swipe_refresher_layout_recent_call.setOnRefreshListener {
            getRecentCallListFromFirebase()
            swipe_refresher_layout_recent_call.isRefreshing = false
        }
        getRecentCallListFromFirebase()


        iv_recent_accept.setOnClickListener(this)
        iv_recent_reject.setOnClickListener(this)
        btn_nav_to_contact.setOnClickListener(this)

        getCalls()


    }

    private fun setUserToCalls() {
        val userValue = hashMapOf(
            auth.uid to "1",
        )
        dbFireStore.collection("userCalls").document(auth.uid!!).set(userValue)
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun getRecentCallListFromFirebase() {
        val documentFirebase = dbFireStore.collection("recentCall").document(auth.currentUser!!.uid)

        documentFirebase.get().addOnSuccessListener { document ->
            if (document.exists()) {

                setUpRecentCallRecyclerView()
                rv_recent_call_list.visibility = View.VISIBLE
                tv_recent_call_list.visibility = View.GONE

                val myCalls = document.get("myCalls") as ArrayList<Map<String, String>>
                for (i in myCalls.size-1 downTo 0) {
                    callList.add(
                        RecentCallModel(
                            myCalls[i]["userId"].toString(),
                            myCalls[i]["userName"].toString(),
                            myCalls[i]["userImage"].toString(),
                            myCalls[i]["userPhone"].toString(),
                            myCalls[i]["date"].toString(),
                            myCalls[i]["callState"].toString()
                        )
                    )
                }
                adapter.notifyDataSetChanged()
            }
        }

    }

    private fun setUpRecentCallRecyclerView(){
        rv_recent_call_list.layoutManager = LinearLayoutManager(this)
        rv_recent_call_list.setHasFixedSize(true)
        callList = arrayListOf()
        adapter = RecentCallsAdapter(this,this,callList)
        rv_recent_call_list.adapter = adapter
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
       if(requestCode == PERMISSION_CALL_REQUEST_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED ){
                if (isCaller){
                    startCallActivity(1)
                }else{
                    startCallActivity(0)
                }
            }else{
                showDialogForPermission()
            }
        }
    }
    // permission dialog
    private fun showDialogForPermission(){
        AlertDialog.Builder(this).setMessage("Look like you turned off permission, To turned on permission").setPositiveButton("Go to Setting"){ _, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package",packageName,null)
                intent.data = uri
                startActivity(intent)
            }catch (e : ActivityNotFoundException){
                e.printStackTrace()
            }
        }.setNegativeButton("Cancel"){dialog,_ ->
            dialog.dismiss()
        }.show()
    }


    override fun onSelectedLRecentCallClick(recentCallModel: RecentCallModel) {
        val sdf = SimpleDateFormat("d MMM,h:mm a")
        currentDate = sdf.format(Date())
        anotherUserUid = recentCallModel.userId.toString()
        anotherUserName = recentCallModel.userName.toString()
        anotherUserPhone = recentCallModel.userPhone.toString()
        anotherUserPhoto = recentCallModel.userImage.toString()
        checkUserOnline()
    }

////

    private fun showDialogForReject(){
        AlertDialog.Builder(this).setMessage("$anotherUserName is reject your call").setPositiveButton("Ok"){ dialog, _ ->
            dialog.dismiss()
        }.show()
    }
    private fun showDialogForOffline(){
        AlertDialog.Builder(this).setMessage("$anotherUserName is currently offline or not receiving").setPositiveButton("Ok"){ dialog, _ ->
            dialog.dismiss()
        }.show()
    }
    private fun showDialogForBusy(){
        AlertDialog.Builder(this).setMessage("$anotherUserName is currently busy").setPositiveButton("Ok"){ dialog, _ ->
            dialog.dismiss()
        }.show()
    }

    private fun checkUserOnline(){

        val document =  dbFireStore.collection("userCalls").document(anotherUserUid)
        document.get().addOnSuccessListener { document ->
            if (document != null) {
                if (document.data?.get(anotherUserUid).toString() == "1") {
                    val userValue = hashMapOf(
                        anotherUserUid to auth.uid!!.toString(),
                    )
                    dbFireStore.collection("userCalls").document(anotherUserUid).set(userValue)
                        .addOnCompleteListener {
                            Log.i("sagar", "checkUserOnline")
                            isCaller = true
                            checkPermissionForCalling(1)
                        }
                }else{
                    showDialogForBusy()
                }
            } else {
                Log.d(ContentValues.TAG, "No such document")
            }
        }

    }




    // permission for calling

    private fun checkPermissionForCalling(caller : Int){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_DENIED){
                val permission = arrayOf(
                    Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
                    , Manifest.permission.MODIFY_AUDIO_SETTINGS)
                requestPermissions(permission, PERMISSION_CALL_REQUEST_CODE);
            }else{
                startCallActivity(caller)
            }
        }
    }

    private fun startCallActivity(caller: Int) {
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra("userId",auth.uid.toString())
        intent.putExtra("anotherUserId",anotherUserUid)
        intent.putExtra("caller",caller)
        startActivityForResult(intent, RESULT_REQUEST_CODE)
    }

    // for call bar accept and reject

    private fun getCalls(){
        val document =  dbFireStore.collection("userCalls").document(auth.uid!!)
        document.addSnapshotListener{snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                if (snapshot.data?.get(auth.uid) != "1"){
                    anotherUserUid = snapshot.data?.get(auth.uid).toString()
                    dbFireStore.collection("userInfo").document(anotherUserUid).get().addOnSuccessListener { document ->

                        val sdf = SimpleDateFormat("d MMM,h:mm a")
                        currentDate = sdf.format(Date())

                        anotherUserName = document.get("userName").toString()
                        anotherUserPhoto = document.get("userImage").toString()
                        anotherUserPhone = document.get("userPhoneNumber").toString()
                        ll_recent_call.visibility = View.VISIBLE
                        tv_recent_incoming_call.text = "$anotherUserName is calling..."
                    }

                }else{
                    ll_recent_call.visibility = View.GONE
                }
            } else {
                Log.d("TAG", "Current data: null")
            }}
    }


    override fun onClick(view: View?) {
        when(view?.id){
            R.id.iv_recent_accept ->{
                setUserCallsForAccept()
            }
            R.id.iv_recent_reject ->{
                ll_recent_call.visibility = View.GONE
                callState = "callMissed"
                setUserToCalls()
                setRecentDataToFireStore(callState)
            }
            R.id.btn_nav_to_contact ->{
                val intent = Intent(this,ContactActivity::class.java)
                startActivityForResult(intent, CONTACT_REQUEST_CODE)
            }
        }
    }

    private fun setUserCallsForAccept() {

        val anotherUserValue = hashMapOf(
            anotherUserUid to auth.uid,
        )
        dbFireStore.collection("userCalls").document(anotherUserUid).set(anotherUserValue).addOnCompleteListener {
            ll_recent_call.visibility = View.GONE
            checkPermissionForCalling(0)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_REQUEST_CODE){
            if (resultCode == RESULT_OK){
                setUserToCalls()
                isCaller = false
                callState = data!!.getStringExtra("callState").toString()
                val condition = data.getStringExtra("condition").toString()
                if (condition == "reject" ){
                    showDialogForReject()
                }else if (condition == "offline"){
                    showDialogForOffline()
                }
                setRecentDataToFireStore(callState)
            }
        }
    }

    private fun setRecentDataToFireStore(callState: String) {

        val myCalls = MyCalls(
            anotherUserUid, anotherUserName, anotherUserPhoto,
            anotherUserPhoto, currentDate, callState
        )
        dbFireStore.collection("recentCall").document(auth.currentUser!!.uid)
            .update("myCalls", FieldValue.arrayUnion(myCalls))
            .addOnCompleteListener { additionTask ->
                if (additionTask.isSuccessful) {
                    getRecentCallListFromFirebase()
                } else {
                    additionTask.exception?.message?.let {
                        Log.e("sagar", it)
                    }
                }
            }.addOnFailureListener { e ->
                Log.i("sagar", e.message.toString())
            }
    }


    // menu for profile navigation

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
       if(item.itemId == R.id.nav_profile){
                val intent = Intent(this,MainActivity::class.java)
                startActivity(intent)
            }
        return super.onOptionsItemSelected(item)
    }


    companion object{
        private const val CONTACT_REQUEST_CODE = 1
        private const val PERMISSION_CALL_REQUEST_CODE = 2
        private const val RESULT_REQUEST_CODE = 3
        private const val RESULT_OK = 4
    }
}