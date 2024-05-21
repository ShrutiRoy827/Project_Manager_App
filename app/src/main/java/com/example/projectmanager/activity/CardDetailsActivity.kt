package com.example.projectmanager.activity

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmanager.R
import com.example.projectmanager.adapter.CardMemberListItemAdapter
import com.example.projectmanager.dialog.ColorListDialog
import com.example.projectmanager.dialog.MemberListDialog
import com.example.projectmanager.firebase.FireStoreClass
import com.example.projectmanager.model.Board
import com.example.projectmanager.model.Card
import com.example.projectmanager.model.SelectedMember
import com.example.projectmanager.model.Task
import com.example.projectmanager.model.User
import com.example.projectmanager.util.Constants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CardDetailsActivity : BaseActivity() {
    private lateinit var mBoardDetails: Board
    private var mTaskListPosition = -1
    private var mCardPosition = -1
    private var mSelectedColor:String = ""
    private lateinit var mMembersDetailList: ArrayList<User>
    private var mSelectedDueDateMS: Long = 0
//    private var mCardDetails = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)
        getIntentData()

        val cardDetailsToolbar = findViewById<Toolbar?>(R.id.toolbar_card_details_activity)
        val title = mBoardDetails
            .taskList[mTaskListPosition]
            .cards[mCardPosition].name
        setActionBar(title, cardDetailsToolbar)

        val cardNameDetailsET = findViewById<AppCompatEditText>(R.id.card_name_details_et)
        cardNameDetailsET.setText(title)
        cardNameDetailsET.setSelection(cardNameDetailsET.text.toString().length)

        mSelectedColor = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].labelColor
        if (mSelectedColor.isNotEmpty()) {
            setColor()
        }

        val selectColorTV = findViewById<TextView>(R.id.select_color_tv)
        selectColorTV.setOnClickListener {
            labelColorListDialog()
        }

        val selectMemberTV = findViewById<TextView>(R.id.select_member_tv)
        selectMemberTV.setOnClickListener {
            membersListDialog()
        }

        setupSelectedMembersList()

        val selectDueDateTV = findViewById<TextView>(R.id.select_due_date_tv)
        selectDueDateTV.setOnClickListener {
            showDatePicker()
        }

        setUpDueDate()

        val updateCardDetailsBtn = findViewById<Button>(R.id.update_card_details_btn)
        updateCardDetailsBtn.setOnClickListener {
            if (cardNameDetailsET.text.toString().isNotEmpty()) {
                updateCardDetails()
            } else {
                Toast.makeText(this, "Enter a card name.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_card_details_delete_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_delete_card ->{
                alertDialogForDeleteList(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION")
    private fun getIntentData() {
        if (intent.hasExtra(Constants.BOARD_DETAILS)) {
            mBoardDetails = intent.getParcelableExtra(Constants.BOARD_DETAILS)!!
        }
        if (intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)) {
            mTaskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)
        }
        if (intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)) {
            mCardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)
        }
        if (intent.hasExtra(Constants.BOARD_MEMBERS_LIST)) {
            mMembersDetailList = intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST)!!
        }
    }

    private fun updateCardDetails() {
        val cardNameDetailsET = findViewById<AppCompatEditText>(R.id.card_name_details_et)
        val card = Card(
            cardNameDetailsET.text.toString(),
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].createdBy,
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo,
            mSelectedColor,
            mSelectedDueDateMS
        )
        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size - 1)

        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition] = card
        showProgressDialog("Please Wait...")
        FireStoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }

    private fun deleteCard() {
        val cardList: ArrayList<Card> = mBoardDetails.taskList[mTaskListPosition].cards
        cardList.removeAt(mCardPosition)

        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size-1)
        taskList[mTaskListPosition].cards = cardList

        showProgressDialog("Please Wait...")
        FireStoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }

    private fun alertDialogForDeleteList(cardName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete $cardName?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes") { dialogInterface, which ->
            dialogInterface.dismiss()
            deleteCard()
        }
        builder.setNegativeButton("No") {
                dialogInterface, which ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun colorList(): ArrayList<String>{
        return ArrayList(resources.getStringArray(R.array.label_colors).asList())
    }

    private fun setColor() {
        val selectColorTV = findViewById<TextView>(R.id.select_color_tv)
        selectColorTV.text = ""
        selectColorTV.setBackgroundColor(Color.parseColor(mSelectedColor))
    }

    private fun labelColorListDialog() {
        val colorList: ArrayList<String> = colorList()
        val listDialog = object : ColorListDialog(
            this@CardDetailsActivity,
            colorList,
            "Select Label Color",
            mSelectedColor
        ) {
            override fun onItemSelected(color: String) {
                mSelectedColor = color
                setColor()
            }
        }
        listDialog.show()
    }

    private fun setupSelectedMembersList() {
        val cardAssignedMemberList = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo
        val selectedMemberList: ArrayList<SelectedMember> = ArrayList()

        val selectMembersTV = findViewById<TextView>(R.id.select_member_tv)
        val selectedMemberListRV = findViewById<RecyclerView>(R.id.selected_member_list_rv)

        for (i in mMembersDetailList.indices) {
            for (j in cardAssignedMemberList) {
                if (mMembersDetailList[i].id == j) {
                    val selectedMember = SelectedMember(
                        mMembersDetailList[i].id,
                        mMembersDetailList[i].image
                    )
                    selectedMemberList.add(selectedMember)
                }
            }
        }

        if (selectedMemberList.size > 0) {

            selectedMemberList.add(SelectedMember("", ""))
            selectMembersTV.visibility = View.GONE
            selectedMemberListRV.visibility = View.VISIBLE

            selectedMemberListRV.layoutManager = GridLayoutManager(this, 6)
            val adapter = CardMemberListItemAdapter(this, selectedMemberList, true)
            selectedMemberListRV.adapter = adapter

            adapter.setOnClickListener(
                object : CardMemberListItemAdapter.OnClickListener{
                    override fun onClick() {
                        membersListDialog()
                    }
                }
            )
        } else {
            selectMembersTV.visibility = View.VISIBLE
            selectedMemberListRV.visibility = View.GONE
        }
    }

    private fun membersListDialog() {
        val cardAssignedMemberList = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo
        if (cardAssignedMemberList.size > 0 ) {
            for (i in mMembersDetailList.indices) {
                for (j in cardAssignedMemberList) {
                    if (mMembersDetailList[i].id == j) {
                        mMembersDetailList[i].selected = true
                    }
                }
            }
        } else {
            for (i in mMembersDetailList.indices) {
                mMembersDetailList[i].selected = false
            }
        }

        val listDialog = object : MemberListDialog(
            this, mMembersDetailList, "Select Member"
        ) {
            override fun onItemSelected(user: User, action: String) {
                if (action == Constants.SELECT) {
                    if (!mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.contains(user.id)) {
                        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.add(user.id)
                    }
                } else {
                    mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.remove(user.id)
                    for ( i in mMembersDetailList.indices) {
                        if (mMembersDetailList[i].id == user.id) {
                            mMembersDetailList[i].selected = false
                        }
                    }
                }
                setupSelectedMembersList()
            }
        }
        listDialog.show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                val sDayOfMonth =
                    if (dayOfMonth < 10) "0$dayOfMonth"
                    else "$dayOfMonth"
                val sMonthOfYear =
                    if ((month + 1) < 10) "0${month + 1}"
                    else "${month + 1}"
                val selectedDate = "$sDayOfMonth/$sMonthOfYear/$year"

                val selectDueDate = findViewById<TextView>(R.id.select_due_date_tv)
                selectDueDate.text = selectedDate

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val theDate = dateFormat.parse(selectedDate)
                mSelectedDueDateMS = theDate!!.time
            }, year, month, day
        )
        datePickerDialog.show()
    }

    private fun setUpDueDate() {
        mSelectedDueDateMS = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].dueDate
       if (mSelectedDueDateMS > 0) {
           val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
           val selectedDate = dateFormat.format(Date(mSelectedDueDateMS))
           val selectDueDate = findViewById<TextView>(R.id.select_due_date_tv)
           selectDueDate.text = selectedDate
       }
    }

    fun addUpdateTaskListSuccess() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }
}