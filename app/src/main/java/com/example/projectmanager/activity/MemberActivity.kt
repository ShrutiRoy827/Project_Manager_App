@file:Suppress("DEPRECATION")

package com.example.projectmanager.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmanager.R
import com.example.projectmanager.adapter.MemberListItemAdapter
import com.example.projectmanager.firebase.FireStoreClass
import com.example.projectmanager.model.Board
import com.example.projectmanager.model.User
import com.example.projectmanager.util.Constants
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MemberActivity : BaseActivity() {
    private lateinit var mBoardDetails: Board
    private lateinit var mAssignedMemberList: ArrayList<User>
    private var anyChangesMade: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member)

        val memberListToolbar = findViewById<Toolbar>(R.id.toolbar_member_activity)
        setActionBar("Members ", memberListToolbar)

        if (intent.hasExtra(Constants.BOARD_DETAILS)) {
            mBoardDetails = intent.getParcelableExtra(Constants.BOARD_DETAILS)!!
        }

        showProgressDialog("Members are Loading...")
        FireStoreClass().getAssignedMembersListDetails(this, mBoardDetails.assignedTo)
    }

    fun setUpMembersList(list: ArrayList<User>) {
        mAssignedMemberList = list
        hideProgressDialog()
        val memberListRV = findViewById<RecyclerView>(R.id.member_list_rv)
        memberListRV.layoutManager = LinearLayoutManager(this@MemberActivity)
        memberListRV.setHasFixedSize(true)

        val adapter = MemberListItemAdapter(this, list)
        memberListRV.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_member_add_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_add_member -> {
                dialogAddMember()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (anyChangesMade) {
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }

    fun memberDetails(user: User) {
        mBoardDetails.assignedTo.add(user.id)
        FireStoreClass().assignMemberToBoard(this@MemberActivity, mBoardDetails, user)
    }


    private fun dialogAddMember() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_member)
        dialog.findViewById<TextView>(R.id.add_tv).setOnClickListener {
            val email = dialog.findViewById<AppCompatEditText>(R.id.email_add_member_et).text.toString()
            if (email.isNotEmpty()) {
                dialog.dismiss()
                showProgressDialog("Please Wait...")
                FireStoreClass().getMemberDetails(this@MemberActivity, email)
            } else {
                Toast.makeText(this, "Please enter member's email address.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.findViewById<TextView>(R.id.cancel_tv).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    fun memberAssignSuccess(user: User) {
        hideProgressDialog()
        mAssignedMemberList.add(user)
        anyChangesMade = true
        setUpMembersList(mAssignedMemberList)

        SendNotificationToUserAsync(mBoardDetails.name, user.fcmToken).execute()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class SendNotificationToUserAsync(
        val boardName: String,
        val token: String)
        : AsyncTask<Any, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog("Please Wait...")
        }
        override fun doInBackground(vararg params: Any?): String {
            var result: String
            var connection: HttpURLConnection? = null
            try {
                val url = URL(Constants.FCM_BASE_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.doOutput = true
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty(Constants.FCM_AUTHORIZATION, "${Constants.FCM_KEY} = ${Constants.FCM_SERVER_KEY}")
                connection.useCaches = false
                val write = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE, "Assigned to the board $boardName")
                dataObject.put(Constants.FCM_KEY_MESSAGE, "You have assigned to the board by ${mAssignedMemberList[0].name}")
                jsonRequest.put(Constants.FCM_KEY_DATA, dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO, token)

                write.writeBytes(jsonRequest.toString())
                write.flush()
                write.close()

                val httpResult: Int = connection.responseCode
                if (httpResult == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    try {
                        while (reader.readLine().also { line = it }!= null) {
                            sb.append(line + "\n")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        try {
                            inputStream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    result = sb.toString()
                } else {
                    result = connection.responseMessage
                }
            } catch (e: SocketTimeoutException) {
                result = "Connection Timeout"
            } catch (e: Exception) {
                result = "Error: " + e.message
            } finally {
                connection?.disconnect()
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            hideProgressDialog()
            Log.e("JSON Response Result ", result!!)
        }
    }
}