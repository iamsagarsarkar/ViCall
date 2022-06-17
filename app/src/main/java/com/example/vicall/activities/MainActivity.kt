package com.example.vicall.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.vicall.R
import com.example.vicall.models.MyCalls
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_image_upload.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var storageReference : StorageReference


    private lateinit var dialog: Dialog
    private var saveImageUri : Uri? = null

    private lateinit var list: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(profile_toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        profile_toolBar.setNavigationOnClickListener {
            onBackPressed()
        }

        auth = Firebase.auth
        dbFireStore = Firebase.firestore
        firebaseStorage = Firebase.storage
        storageReference = firebaseStorage.reference

        btn_signOut.setOnClickListener {
            auth.signOut()
            reload()
        }


        getUserDataFromFireStore(auth.currentUser)


        btn_edit_user_image.setOnClickListener{
            dialog = Dialog(this)
            dialog.setContentView(R.layout.item_image_upload)
            dialog.iv_gallery.setOnClickListener {
                chooseFromGallery()
                dialog.cancel()
            }
            dialog.iv_camera.setOnClickListener {
                takeFromCamera()
                dialog.cancel()
            }
            dialog.show()
        }

        btn_update_user_image.setOnClickListener {
            btn_update_user_image.isEnabled = false
            updateImageInFirestore(saveImageUri.toString())
        }

    }


    private fun reload() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()

    }

    override fun onStart() {
        super.onStart()
        getRecentCallListFromFirebase()
    }

// get user data from firebase

    @SuppressLint("SetTextI18n")
    private fun getUserDataFromFireStore(user: FirebaseUser?){
        val document = dbFireStore.collection("userInfo").document(user!!.uid)
        document.addSnapshotListener { value, error ->
            if (error != null) {
                Log.i("userInfo", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (value != null && value.exists()){
                tv_profile_name.text = value.getString("userName")
                tv_profile_phone.text = "Ph: "+value.getString("userPhoneNumber")
                getBitmapFromURL(value.getString("userImage").toString())
            }
        }
    }

    private fun getBitmapFromURL(imageURL: String){
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        executor.execute {
            try {
                val `in` = URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                handler.post {
                    iv_profile.setImageBitmap(image)
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }





    // image upload

    private fun chooseFromGallery(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permissions, PERMISSION_CODE_GALLERY)
            }else{
                galleryIntent()
            }
        }

    }

    private fun takeFromCamera(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ){
                val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,android.Manifest.permission.CAMERA)
                requestPermissions(permissions, PERMISSION_CODE_CAMERA)
            }else{
                cameraIntent()
            }
        }
    }
    private fun galleryIntent(){
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, ADD_IMAGE_REQUEST_CODE_GALLERY)
    }
    private fun cameraIntent(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, ADD_IMAGE_REQUEST_CODE_CAMERA)
    }

    private fun saveImageToStorage(imageBitmap: Bitmap) : Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")
        try {
            val stream : OutputStream = FileOutputStream(file)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e : IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE_GALLERY ->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    galleryIntent()
                }else{
                    showDialogForPermission()
                }
            }
            PERMISSION_CODE_CAMERA ->{
                if (grantResults.isNotEmpty() && grantResults[2] == PackageManager.PERMISSION_GRANTED){
                    cameraIntent()
                }else{
                    showDialogForPermission()
                }
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



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode ==ADD_IMAGE_REQUEST_CODE_GALLERY){
                if (data != null){
                    val uri = data.data
                    try {
                        val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
                        saveImageUri =  saveImageToStorage(imageBitmap)
                        uploadImageToFireStorage(saveImageUri.toString())
                        iv_profile.setImageBitmap(imageBitmap)
                    }catch (e : IOException){
                        e.printStackTrace()
                    }
                }
            }else if(requestCode == ADD_IMAGE_REQUEST_CODE_CAMERA){
                val imageBitmap = data!!.extras!!.get("data") as Bitmap
                saveImageUri = saveImageToStorage(imageBitmap)
                uploadImageToFireStorage(saveImageUri.toString())
                iv_profile.setImageBitmap(imageBitmap)
            }
        }
    }



    private fun updateImageInFirestore(imagePath: String?){

        val file = Uri.fromFile(File(imagePath!!))
        val reference = storageReference.child("images/${file.lastPathSegment}")

        reference.downloadUrl.addOnSuccessListener { url ->
            dbFireStore.collection("userInfo").document(auth.currentUser!!.uid).update("userImage",url.toString())
                .addOnSuccessListener {
                    if(list.size>0){
                        updateAllRecent(url.toString())
                    }else {
                        cardView_update_profile_image.visibility = View.GONE
                        Toast.makeText(
                            this,
                            "Profile picture successfully updated",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.addOnFailureListener { e -> Log.w("userData", "Error updating document", e) }
        }.addOnFailureListener {
                e -> Log.i("path", "Error occurred during upload file", e)
        }
    }

    private fun uploadImageToFireStorage(imagePath: String){
        Toast.makeText(this,"Wait few moment", Toast.LENGTH_SHORT).show()
        val file = Uri.fromFile(File(imagePath))
        val reference = storageReference.child("images/${file.lastPathSegment}")
        val uploadTask = reference.putFile(file)
        uploadTask.addOnFailureListener { e -> Log.w("userData", "Error occurred during upload file", e) }.addOnSuccessListener { taskSnapshot ->
            btn_update_user_image.isEnabled = true
            cardView_update_profile_image.visibility = View.VISIBLE
        }

    }

    private fun updateAllRecent(imagePath: String){
       for (i in 0 until list.size){
           updateRecentCall(imagePath,list[i])
           if (i == list.size-1){
               cardView_update_profile_image.visibility = View.GONE
               Toast.makeText(
                   this,
                   "Profile picture successfully updated",
                   Toast.LENGTH_SHORT
               ).show()
           }
       }
    }

    private fun updateRecentCall(imagePath: String,anotherUserId:String) {
        val documentFirebase = dbFireStore.collection("recentCall").document(anotherUserId)

        documentFirebase.get().addOnSuccessListener { document ->
            if (document.exists()) {

                val myCalls = document.get("myCalls") as ArrayList<Map<String, String>>
                for (i in 0 until myCalls.size) {
                    val myCall = MyCalls(
                        myCalls[i]["userId"].toString(),
                        myCalls[i]["userName"].toString(),
                        myCalls[i]["userImage"].toString(),
                        myCalls[i]["userPhone"].toString(),
                        myCalls[i]["date"].toString(),
                        myCalls[i]["callState"].toString()
                    )
                    documentFirebase.update("myCalls",FieldValue.arrayRemove(myCall))
                    if (myCall.userId == auth.currentUser!!.uid){
                        myCall.userImage = imagePath
                    }
                    documentFirebase.update("myCalls",FieldValue.arrayUnion(myCall))
                }
            }
        }
    }



    private fun getRecentCallListFromFirebase() {
        list  = arrayListOf()
        val documentFirebase = dbFireStore.collection("recentCall").document(auth.currentUser!!.uid)

        documentFirebase.get().addOnSuccessListener { document ->
            if (document.exists()) {

                val myCalls = document.get("myCalls") as ArrayList<Map<String, String>>
                for (i in myCalls.size-1 downTo 0) {
                    if (!list.contains(myCalls[i]["userId"].toString()))
                   list.add(myCalls[i]["userId"].toString())
                }
            }
        }

    }


    companion object{
        private const val ADD_IMAGE_REQUEST_CODE_GALLERY = 1
        private const val ADD_IMAGE_REQUEST_CODE_CAMERA = 2
        private const val PERMISSION_CODE_GALLERY = 3
        private const val PERMISSION_CODE_CAMERA = 4
        private const val IMAGE_DIRECTORY = "Vicall"
    }
}