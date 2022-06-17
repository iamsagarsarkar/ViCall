package com.example.vicall.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vicall.R
import com.example.vicall.adapters.ContactAdapter
import com.example.vicall.models.ContactModel
import com.example.vicall.models.MyCalls
import com.example.vicall.onClick.CallOnClick
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.android.synthetic.main.activity_recent_call.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class ContactActivity : AppCompatActivity(), CallOnClick, View.OnClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore
    private lateinit var adapter: ContactAdapter
    private lateinit var userList : ArrayList<ContactModel>
    private var anotherUserUid = ""
    private lateinit var anotherUserName : String
    private lateinit var anotherUserPhoto : String
    private lateinit var anotherUserPhone : String
    private lateinit var currentDate : String

    private lateinit var callState : String


    private var isCaller : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(activity_contact_list_toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity_contact_list_toolBar.setNavigationOnClickListener {
            onBackPressed()
        }


        auth = Firebase.auth
        dbFireStore = FirebaseFirestore.getInstance()

        swipe_refresher_layout_contact.setOnRefreshListener {
            loadContacts()
            swipe_refresher_layout_contact.isRefreshing = false
        }

        iv_contact_accept.setOnClickListener(this)
        iv_contact_reject.setOnClickListener(this)


        getCalls()
    }

    private fun setUserToCalls() {
        val userValue = hashMapOf(
            auth.uid to "1",
        )
        dbFireStore.collection("userCalls").document(auth.uid!!).set(userValue)
    }


    override fun onStart() {
        super.onStart()
        loadContacts()
    }

    private fun getUserListFromFirebase(hashSet: HashSet<String>) {
        dbFireStore.collection("userInfo").orderBy("userName", Query.Direction.ASCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("fireStoreError", error.message.toString())
                        return
                    }

                    setUpContactRecyclerView()
                    rv_contact_list.visibility = View.VISIBLE
                    tv_empty_list.visibility = View.GONE

                    for (documentChange: DocumentChange in value?.documentChanges!!) {
                        if (documentChange.type == DocumentChange.Type.ADDED) {
                            if ( documentChange.document.id != auth.uid
                                && hashSet.contains(documentChange.document.getString("userPhoneNumber"))){
                                userList.add(ContactModel(
                                    documentChange.document.id,
                                    documentChange.document.getString("userName"),
                                    documentChange.document.getString("userImage"),
                                    documentChange.document.getString("userPhoneNumber")))
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

            })
    }

    private fun setUpContactRecyclerView(){
        rv_contact_list.layoutManager = LinearLayoutManager(this)
        rv_contact_list.setHasFixedSize(true)
        userList = arrayListOf()
        adapter = ContactAdapter(this,this,userList)
        rv_contact_list.adapter = adapter
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE_CONTACT){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                connectContactAndFirestore()
            }else{
                showDialogForPermission()
            }
        }else if(requestCode == PERMISSION_CALL_REQUEST_CODE){
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

    // user contact fetch


    private fun loadContacts() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED){
                val permissions = arrayOf(Manifest.permission.READ_CONTACTS)
                requestPermissions(permissions, PERMISSION_CODE_CONTACT)
            }else{
                connectContactAndFirestore()
            }
        }
    }

    private fun connectContactAndFirestore() {
        val hashSet: HashSet<String> = getContacts()
        if (hashSet.isNotEmpty()){
            getUserListFromFirebase(hashSet)
        }
    }


    @SuppressLint("Range")
    private fun getContacts() : HashSet<String>{

       val hashSet: HashSet<String> = hashSetOf()

        val resolver: ContentResolver = contentResolver;
        val cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null,
            null)

        if (cursor != null) {
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                    val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    val phoneNumber = (cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))).toInt()

                    if (phoneNumber > 0) {
                        val cursorPhone = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", arrayOf(id), null)

                        if (cursorPhone != null) {
                            if(cursorPhone.count > 0) {
                                while (cursorPhone.moveToNext()) {
                                    var phoneNumValue = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                    if (!phoneNumValue.startsWith("+91")){
                                       phoneNumValue = "+91$phoneNumValue"
                                    }
                                    hashSet.add(phoneNumValue)
                                }
                            }
                        }
                        cursorPhone?.close()
                    }
                }
            } else {
               Log.i("contact","no contact")
            }
        }
        cursor?.close()
        return hashSet
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

    override fun onSelectedLCallClick(contactModel: ContactModel) {

        val sdf = SimpleDateFormat("d MMM,h:mm a")
        currentDate = sdf.format(Date())

        anotherUserUid = contactModel.userId.toString()
        anotherUserName = contactModel.userName.toString()
        anotherUserPhoto = contactModel.userImage.toString()
        anotherUserPhone = contactModel.userPhone.toString()

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
                }else if (document.data?.get(anotherUserUid).toString() == "0"){
                    showDialogForOffline()
                }else{
                    showDialogForBusy()
                }
            } else {
                Log.d(TAG, "No such document")
            }
        }

    }


    // permission for calling

    private fun checkPermissionForCalling(caller : Int){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_DENIED){
                val permission = arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO
                    ,Manifest.permission.MODIFY_AUDIO_SETTINGS)
                requestPermissions(permission, PERMISSION_CALL_REQUEST_CODE)
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
                        ll_contact_call.visibility = View.VISIBLE
                        tv_contact_incoming_call.text = "$anotherUserName is calling..."
                    }

                }else{
                    ll_contact_call.visibility = View.GONE
                }
            } else {
                Log.d("TAG", "Current data: null")
            }}
    }


    override fun onClick(view: View?) {
        when(view?.id){
            R.id.iv_contact_accept ->{
               setUserCallsForAccept()
            }
            R.id.iv_contact_reject ->{
             ll_contact_call.visibility = View.GONE
                callState = "callMissed"
                setUserToCalls()
                setRecentDataToFireStore(callState)
            }
        }
    }

    private fun setUserCallsForAccept() {

        val anotherUserValue = hashMapOf(
            anotherUserUid to auth.uid,
        )
            dbFireStore.collection("userCalls").document(anotherUserUid).set(anotherUserValue).addOnCompleteListener {
                ll_contact_call.visibility = View.GONE
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
               if (condition == "rejected" ){
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
                    Log.d("sagar", "Update complete.")
                } else {
                    additionTask.exception?.message?.let {
                        Log.e("sagar", it)
                    }
                }
            }.addOnFailureListener { e ->
            Log.i("sagar", e.message.toString())
        }
    }

    companion object{
        private const val PERMISSION_CODE_CONTACT = 1
        private const val PERMISSION_CALL_REQUEST_CODE = 2
        private const val RESULT_REQUEST_CODE = 3
        private const val RESULT_OK = 4
    }

}