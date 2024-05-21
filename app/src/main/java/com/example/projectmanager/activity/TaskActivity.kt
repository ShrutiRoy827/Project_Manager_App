package com.example.projectmanager.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmanager.R
import com.example.projectmanager.adapter.TaskListItemAdapter
import com.example.projectmanager.firebase.FireStoreClass
import com.example.projectmanager.model.Board
import com.example.projectmanager.model.Card
import com.example.projectmanager.model.Task
import com.example.projectmanager.model.User
import com.example.projectmanager.util.Constants

class TaskActivity : BaseActivity() {
    private lateinit var mBoardDetails: Board
    private lateinit var mBoardDocumentID: String
    lateinit var mAssignedMemberDetails: ArrayList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        if (intent.hasExtra(Constants.DOCUMENT_ID)) {
            mBoardDocumentID = intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }
        showProgressDialog("Tasks are loading...")
        FireStoreClass().getBoardDetails(this@TaskActivity, mBoardDocumentID)
    }

    fun boardDetails(board: Board) {
        mBoardDetails = board
        hideProgressDialog()

        val taskListToolbar = findViewById<Toolbar?>(R.id.toolbar_task_activity)
        setActionBar(mBoardDetails.name, taskListToolbar)

        showProgressDialog("Please Wait...")
        FireStoreClass().getAssignedMembersListDetails(this, mBoardDetails.assignedTo)
    }

    fun addUpdateTaskListSuccess() {
        hideProgressDialog()
        showProgressDialog("Please Wait...")
        FireStoreClass().getBoardDetails(this@TaskActivity, mBoardDetails.documentId)
    }

    private val startUpdateActivityAndGetResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showProgressDialog("Please Wait...")
            FireStoreClass().getBoardDetails(this@TaskActivity, mBoardDetails.documentId)
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    fun cardDetails(taskListPosition: Int, cardPosition: Int) {
        val intent = Intent(this@TaskActivity, CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAILS, mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION, taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION, cardPosition)
        intent.putExtra(Constants.BOARD_MEMBERS_LIST, mAssignedMemberDetails)

        startUpdateActivityAndGetResult.launch(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_task_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_members -> {
                val intent = Intent(this, MemberActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAILS, mBoardDetails)
                startUpdateActivityAndGetResult.launch(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun createTaskList(taskListName: String) {
        val task = Task(taskListName, FireStoreClass().getCurrentUserid())
        mBoardDetails.taskList.add(0, task)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog("Please Wait...")
        FireStoreClass().addUpdateTaskList(this, mBoardDetails)
    }

    fun updateTaskList(position: Int, listName: String, model: Task) {
        val task = Task(listName, model.createdBy, model.cards)
        mBoardDetails.taskList[position] = task
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog("Please Wait...")
        FireStoreClass().addUpdateTaskList(this, mBoardDetails)
    }

    fun deleteTaskList(position: Int) {
        mBoardDetails.taskList.removeAt(position)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog("Please Wait...")
        FireStoreClass().addUpdateTaskList(this, mBoardDetails)
    }

    fun addCardToTaskList(position: Int, cardName: String) {
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)

        val cardAssignedUserList: ArrayList<String> = ArrayList()
        cardAssignedUserList.add(FireStoreClass().getCurrentUserid())

        val card = Card(cardName, FireStoreClass().getCurrentUserid(), cardAssignedUserList)

        val cardList = mBoardDetails.taskList[position].cards
        cardList.add(card)

        val task = Task(
            mBoardDetails.taskList[position].title,
            mBoardDetails.taskList[position].createdBy,
            cardList
        )

        mBoardDetails.taskList[position] = task
        showProgressDialog("Please Wait...")
        FireStoreClass().addUpdateTaskList(this, mBoardDetails)
    }

    fun boardMemberDetailsList(list: ArrayList<User>) {
        mAssignedMemberDetails = list
        hideProgressDialog()
        val addTaskList = Task()
        mBoardDetails.taskList.add(addTaskList)

        val taskListRV = findViewById<RecyclerView?>(R.id.task_list_rv)
        taskListRV.layoutManager = LinearLayoutManager(this@TaskActivity, LinearLayoutManager.HORIZONTAL, false)
        taskListRV.setHasFixedSize(true)

        val adapter = TaskListItemAdapter(this@TaskActivity, mBoardDetails.taskList)
        taskListRV.adapter = adapter
    }

    fun updateCardInTaskList(taskListPosition: Int, cards: ArrayList<Card>) {
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size -1)
        mBoardDetails.taskList[taskListPosition].cards = cards
        showProgressDialog("Please wait a moment...")
        FireStoreClass().addUpdateTaskList(this, mBoardDetails)
    }
}