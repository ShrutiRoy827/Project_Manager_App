package com.example.projectmanager.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.adapter.BoardItemsAdapter
import com.example.projectmanager.firebase.FireStoreClass
import com.example.projectmanager.model.Board
import com.example.projectmanager.model.User
import com.example.projectmanager.util.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mUserName: String
    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val drawerLayout = findViewById<DrawerLayout?>(R.id.drawer_layout)
        val navView = findViewById<NavigationView?>(R.id.nav_view)
        val toolbarMainActivity = findViewById<Toolbar?>(R.id.toolbar_main_activity)
        val createBoardFAB = findViewById<FloatingActionButton?>(R.id.create_board_fab)

        setActionBar(toolbarMainActivity, drawerLayout)

        navView.setNavigationItemSelectedListener(this)

        mSharedPreferences = this.getSharedPreferences(Constants.PROJECT_MANAGER_PREFERENCES, Context.MODE_PRIVATE)

        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)

        if (tokenUpdated) {
            showProgressDialog("Please wait a moment...")
            FireStoreClass().loadedUserDetails(this, true)
        } else {
            FirebaseMessaging.getInstance().token.addOnSuccessListener(this@MainActivity) {
                updateFCMToken(it)
            }
        }

        FireStoreClass().loadedUserDetails(this@MainActivity, true)

        createBoardFAB.setOnClickListener {
            val intent = Intent(this@MainActivity, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUserName)
            boardLauncher.launch(intent)
        }
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout?>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            doubleBackToExit()
        }
    }

    private val boardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            FireStoreClass().getBoardsList(this)
        }
    }

    private val startUpdateActivityAndGetResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            FireStoreClass().loadedUserDetails(this)
        } else {
            Log.e("onActivityResult()", "Profile update cancelled by user")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawerLayout = findViewById<DrawerLayout?>(R.id.drawer_layout)
        when (item.itemId) {
            R.id.nav_my_profile -> {
                startUpdateActivityAndGetResult.launch(Intent(this@MainActivity, MyProfileActivity::class.java))
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply()
                val intent = Intent(this@MainActivity, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun populateBoardListToUI(boardList: ArrayList<Board>) {
        val boardListRV = findViewById<RecyclerView?>(R.id.board_list_rv)
        val noBoardTV = findViewById<TextView?>(R.id.no_board_tv)
        hideProgressDialog()
        if (boardList.size > 0) {
            boardListRV.visibility = View.VISIBLE
            noBoardTV.visibility = View.GONE
            boardListRV.layoutManager = LinearLayoutManager(this@MainActivity)
            boardListRV.setHasFixedSize(true)

            val adapter = BoardItemsAdapter(this, boardList)
            boardListRV.adapter = adapter

            adapter.seOnClickListener(object : BoardItemsAdapter.OnClickListener {
                override fun onClick(position: Int, board: Board) {
                    val intent = Intent(this@MainActivity,TaskActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, board.documentId)
                    startActivity(intent)
                }
            })
        } else {
            boardListRV.visibility = View.GONE
            noBoardTV.visibility = View.VISIBLE
        }
    }

    private fun setActionBar(toolbar: Toolbar, drawerLayout: DrawerLayout) {
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_nav_menu)
        toolbar.setNavigationOnClickListener {
            toggleDrawer(drawerLayout)
        }
    }

    private fun toggleDrawer(drawerLayout: DrawerLayout) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    fun updateNavigationDetail(user: User, readBoardList: Boolean) {
        hideProgressDialog()
        mUserName = user.name
        val headerView = findViewById<NavigationView?>(R.id.nav_view)
        val navUserImage = findViewById<CircleImageView?>(R.id.nav_user_image)
        val userNameTV = headerView.findViewById<TextView?>(R.id.user_name_tv)
        Glide
            .with(this@MainActivity)
            .load(user.image.toUri().buildUpon().scheme("https").build())
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(navUserImage)
        userNameTV.text = user.name

        if (readBoardList) {
            showProgressDialog("Boards are loading...")
            FireStoreClass().getBoardsList(this)
        }
    }

    fun tokenUpdateSuccess() {
        hideProgressDialog()
        val editor: SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog("Please wait a moment...")
        FireStoreClass().loadedUserDetails(this, true)
    }

    private fun updateFCMToken(token: String) {
        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token
        showProgressDialog("Please wait a moment...")
        FireStoreClass().updateUserProfile(this, userHashMap)
    }
}