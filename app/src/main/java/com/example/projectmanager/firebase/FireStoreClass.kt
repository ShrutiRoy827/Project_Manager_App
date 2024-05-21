package com.example.projectmanager.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.projectmanager.activity.CardDetailsActivity
import com.example.projectmanager.activity.CreateBoardActivity
import com.example.projectmanager.activity.MainActivity
import com.example.projectmanager.activity.MemberActivity
import com.example.projectmanager.activity.MyProfileActivity
import com.example.projectmanager.activity.SignInActivity
import com.example.projectmanager.activity.SignUpActivity
import com.example.projectmanager.activity.TaskActivity
import com.example.projectmanager.model.Board
import com.example.projectmanager.model.User
import com.example.projectmanager.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FireStoreClass {
    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFireStore.collection(Constants.USERS).document(getCurrentUserid()).set(userInfo, SetOptions.merge()).addOnSuccessListener {
                activity.userRegisteredSuccess()
        }.addOnFailureListener {
            Log.e(activity.javaClass.simpleName, "Error is found")
        }
    }

    fun createBoard(activity: CreateBoardActivity, board: Board) {
        mFireStore.collection(Constants.BOARDS).document().set(board, SetOptions.merge()).addOnSuccessListener {
            Log.e(activity.javaClass.simpleName, "Board created successfully.")
            Toast.makeText(activity, "Board created successfully.", Toast.LENGTH_SHORT).show()
            activity.boardCreatedSuccess()
        }.addOnFailureListener {exception ->
            activity.hideProgressDialog()
            Log.e(activity.javaClass.simpleName, "Error while creating a board.", exception)
            Toast.makeText(activity,"Failed to create a board", Toast.LENGTH_SHORT).show()
        }
    }

   fun loadedUserDetails(activity: Activity, readBoardList: Boolean = false) {
       mFireStore.collection(Constants.USERS).document(getCurrentUserid()).get().addOnSuccessListener { document ->
           Log.e(activity.javaClass.simpleName, document.toString())
           val loggedInUser = document.toObject(User::class.java)!!
           when(activity) {
               is SignInActivity -> {
                   activity.signInSuccess(loggedInUser)
               }
               is MainActivity -> {
                   activity.updateNavigationDetail(loggedInUser, readBoardList)
               }
               is MyProfileActivity -> {
                    activity.setUserDataInUI(loggedInUser)
               }
           }
       }.addOnFailureListener { e ->
           when(activity) {
               is SignInActivity -> {
                   activity.hideProgressDialog()
               }
               is MainActivity -> {
                   activity.hideProgressDialog()
               }
               is MyProfileActivity -> {
                   activity.hideProgressDialog()
               }
           }
           Log.e( activity.javaClass.simpleName, "Error while getting loggedIn user details", e)
       }
   }

    fun getBoardsList(activity: MainActivity) {
        mFireStore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserid())
            .get()
            .addOnSuccessListener {document ->
                Log.e(activity.javaClass.simpleName, document.documents.toString())
                val boardList: ArrayList<Board> = ArrayList()
                for (i in document.documents) {
                    val board = i.toObject(Board::class.java)
                    board!!.documentId = i.id
                    boardList.add(board)
                }
                activity.populateBoardListToUI(boardList)
            }.addOnFailureListener { exception ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating a board.", exception)
            }
    }

    fun getBoardDetails(activity: TaskActivity, documentId: String) {
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                Log.e(activity.javaClass.simpleName, document.toString())
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id
                activity.boardDetails(board)
            }.addOnFailureListener { exception ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while loading board.", exception)
            }
    }

    fun updateUserProfile(activity: Activity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserid())
            .update(userHashMap)
            .addOnSuccessListener {
                when(activity){
                    is MainActivity ->
                        activity.tokenUpdateSuccess()
                    is MyProfileActivity ->
                        activity.profileUpdateSuccess()
                }
                Log.i(activity.javaClass.simpleName, "Profile Data updated successfully!")
                Toast.makeText(activity, "Your profile updated successfully!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                when(activity){
                    is MainActivity ->
                        activity.hideProgressDialog()
                    is MyProfileActivity ->
                        activity.hideProgressDialog()
                }
                Log.i(activity.javaClass.simpleName, "Error while creating a board.", e)
                Toast.makeText(activity, "Error while updating your profile!", Toast.LENGTH_SHORT).show()
            }
    }

    fun addUpdateTaskList(activity: Activity, board: Board) {
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList
        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "TaskList updated successfully.")
                if (activity is TaskActivity)
                    activity.addUpdateTaskListSuccess()
                else if (activity is CardDetailsActivity)
                    activity.addUpdateTaskListSuccess()
            }.addOnFailureListener { exception ->
                if (activity is TaskActivity)
                    activity.hideProgressDialog()
                else if (activity is CardDetailsActivity)
                    activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while updating taskList.", exception)
            }
    }

    fun getCurrentUserid(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if (currentUser != null) {
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun getAssignedMembersListDetails(activity: Activity, assignedTo: ArrayList<String>) {
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo)
            .get()
            .addOnSuccessListener { document ->
                Log.e(activity.javaClass.simpleName, document.documents.toString())
                val userList: ArrayList<User> = ArrayList()
                for (i in document.documents) {
                    val user = i.toObject(User::class.java)!!
                    userList.add(user)
                }
                if (activity is MemberActivity)
                    activity.setUpMembersList(userList)
                else if (activity is TaskActivity)
                    activity.boardMemberDetailsList(userList)
            } .addOnFailureListener { exception ->
                if (activity is MemberActivity)
                    activity.hideProgressDialog()
                else if (activity is TaskActivity)
                    activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while loading members.", exception)
            }
    }

    fun getMemberDetails(activity: MemberActivity, email: String) {
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email)
            .get()
            .addOnSuccessListener {document ->
                if (document.documents.size > 0) {
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                } else {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No such member found.")
                }
            }.addOnFailureListener {exception ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while getting user details.", exception)
            }
    }

    fun assignMemberToBoard(activity: MemberActivity, board: Board, user: User) {
        val assignedToHashMap = HashMap<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo
        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)
            }.addOnFailureListener { exception ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while getting user details.", exception)
            }
    }
}