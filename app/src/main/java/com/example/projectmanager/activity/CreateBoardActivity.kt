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
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.firebase.FireStoreClass
import com.example.projectmanager.model.Board
import com.example.projectmanager.util.Constants
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException
import kotlin.collections.ArrayList

class CreateBoardActivity : BaseActivity() {
    companion object {
        private const val READ_STORAGE_PERMISSION_CODE = 1
    }

    private var mSelectedImageFileUri: Uri? = null
    private lateinit var mUserName: String
    private var mBoardImageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_board)

        val createBoardToolbar = findViewById<Toolbar?>(R.id.toolbar_board_activity)
        setActionBar("Create Board", createBoardToolbar)

        if (intent.hasExtra(Constants.NAME)) {
            mUserName = intent.getStringExtra(Constants.NAME)!!
        }

        val boardImageIV = findViewById<CircleImageView>(R.id.board_image_iv)
        boardImageIV.setOnClickListener {
            checkStoragePermissionAndSelectImage()
        }

        val createBoardBtn = findViewById<Button>(R.id.create_board_btn)
        createBoardBtn.setOnClickListener {

            if (mSelectedImageFileUri != null) {
                uploadBoardImage()
            } else {
                showProgressDialog("Please Wait...")
                createBoard()
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
        val boardImageIV = findViewById<CircleImageView?>(R.id.board_image_iv)
        if (result.resultCode == Activity.RESULT_OK && data!!.data != null) {
            val selectedImageUri = data.data
            if (selectedImageUri != null) {
                mSelectedImageFileUri = selectedImageUri
                try {
                    Glide.with(this@CreateBoardActivity)
                        .load(mSelectedImageFileUri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_board_place_holder)
                        .into(boardImageIV!!)
                } catch (e : IOException) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(
                    this@CreateBoardActivity,
                    "Failed to retrieve image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun uploadBoardImage() {
        showProgressDialog("Please Wait...")
        if (mSelectedImageFileUri != null) {
            val fileName = "board_${System.currentTimeMillis()}.${getFileExtension(mSelectedImageFileUri!!)}"
            val storageRef = FirebaseStorage.getInstance().getReference("Boards/${getCurrentUserID()}/$fileName")
            storageRef.putFile(mSelectedImageFileUri!!)
                .addOnSuccessListener {
                    // Image upload successful, get the download URL
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        mBoardImageUrl = uri.toString()
                        Log.d("CreateBoardActivity", "Image Download URL: $mBoardImageUrl") // Log the image URL
                        createBoard() // Now that you have the image URL, create the board
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to upload your image", Toast.LENGTH_SHORT).show()
                        hideProgressDialog()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to upload your image", Toast.LENGTH_SHORT).show()
                    hideProgressDialog()
                }
        }
    }

    private fun createBoard() {
        val assignedUsers: ArrayList<String> = ArrayList()
        val boardNameET = findViewById<EditText>(R.id.board_name_et)
        assignedUsers.add(getCurrentUserID())
        val board = Board(
            boardNameET.text.toString(),
            mBoardImageUrl,
            mUserName,
            assignedUsers
        )
        FireStoreClass().createBoard(this, board)
    }

    fun boardCreatedSuccess() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }
}