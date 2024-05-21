package com.example.projectmanager.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.firebase.FireStoreClass
import com.example.projectmanager.model.User
import com.example.projectmanager.util.Constants
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException

class MyProfileActivity : BaseActivity() {
    companion object {
        private const val READ_STORAGE_PERMISSION_CODE = 1
    }
    private var mSelectedImageFileUri: Uri? = null
    private lateinit var mUserDetails: User
    private var mProfileImageURL: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        val tooBarMyProfile = findViewById<Toolbar?>(R.id.toolbar_my_profile)
        setActionBar("My Profile", tooBarMyProfile)

        FireStoreClass().loadedUserDetails(this@MyProfileActivity)

        val userImageIV = findViewById<CircleImageView?>(R.id.user_image_iv)
        userImageIV.setOnClickListener {
            checkStoragePermissionAndSelectImage()
        }
        val btnUpdate = findViewById<Button?>(R.id.update_btn)
        btnUpdate.setOnClickListener {
            if (mSelectedImageFileUri != null) {
                uploadUserImage()
            } else {
                showProgressDialog("Please Wait...")
                updateUserProfile()
            }
        }
    }

    private fun checkStoragePermissionAndSelectImage() {
        if (ContextCompat.checkSelfPermission(this,
                if (Build.VERSION.SDK_INT >= 33) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }) == PackageManager.PERMISSION_GRANTED
        ) {
            showImageChooser()
        } else {
            ActivityCompat.requestPermissions(
                this,
                if (Build.VERSION.SDK_INT >= 33) {
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }, READ_STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult( requestCode: Int, permissions: Array<out String>, grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImageChooser()
            } else {
                Toast.makeText(
                    this,
                    "Oops, You denied the permission for storage. You can allow it from settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showImageChooser() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(galleryIntent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        val userImageIV = findViewById<CircleImageView?>(R.id.user_image_iv)
        if (result.resultCode == Activity.RESULT_OK && data!!.data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                mSelectedImageFileUri = selectedImageUri
                try {
                    Glide.with(this@MyProfileActivity)
                        .load(mSelectedImageFileUri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(userImageIV!!)
                } catch (e : IOException) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(
                    this@MyProfileActivity,
                    "Failed to retrieve image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun setUserDataInUI(user: User) {
        mUserDetails = user
        val userImageIV = findViewById<CircleImageView?>(R.id.user_image_iv)
        val nameET = findViewById<AppCompatEditText?>(R.id.name_et)
        val emailET = findViewById<AppCompatEditText?>(R.id.email_et)
        val mobileET = findViewById<AppCompatEditText?>(R.id.mobile_et)

        Glide
            .with(this@MyProfileActivity)
            .load(user.image.toUri().buildUpon().scheme("https").build())
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(userImageIV!!)

        nameET.setText(user.name)
        emailET.setText(user.email)
        mobileET.setText(user.mobile)

//        if (user.mobile != 0L) {
//            mobileET.setText(user.mobile.toString())
//        }
    }

    private fun uploadUserImage() {
        showProgressDialog("Please Wait...")
        if (mSelectedImageFileUri != null) {
            val fileName = "profile_${System.currentTimeMillis()}.${getFileExtension(mSelectedImageFileUri!!)}"
            val storageRef = FirebaseStorage.getInstance().getReference("Users/${getCurrentUserID()}/$fileName")
            storageRef.putFile(mSelectedImageFileUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        mProfileImageURL = uri.toString()
                        Log.d("MyProfileActivity", "Image Download URL: $mProfileImageURL") // Log the image URL
                        updateUserProfile() // Update user profile with the image URL
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                        hideProgressDialog()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    hideProgressDialog()
                }
        }
    }

    private fun updateUserProfile() {
        val nameET = findViewById<AppCompatEditText?>(R.id.name_et)
        val mobileET = findViewById<AppCompatEditText?>(R.id.mobile_et)

        val newName = nameET.text.toString()
        val newMobile = mobileET.text.toString()
//            .toLongOrNull() ?: 0L

        val userHashMap = HashMap<String, Any>()

        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageURL
        }

        if (newName != mUserDetails.name) {
            userHashMap[Constants.NAME] = newName
        }

        if (newMobile != mUserDetails.mobile) {
            userHashMap[Constants.MOBILE] = newMobile
        }

        FireStoreClass().updateUserProfile(this@MyProfileActivity, userHashMap)
    }

    fun profileUpdateSuccess() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }
}