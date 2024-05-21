package com.example.projectmanager.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmanager.R
import com.example.projectmanager.adapter.MemberListItemAdapter
import com.example.projectmanager.model.User

abstract class MemberListDialog(
    context: Context,
    private var list: ArrayList<User>,
    private val title: String = ""
): Dialog(context) {
    private var adapter: MemberListItemAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_list, null)
        setContentView(view)
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        setUpRecyclerView(view)
    }

    private fun setUpRecyclerView(view: View) {
        val listTitleTV = view.findViewById<TextView>(R.id.list_title_tv)
        val listRV = view.findViewById<RecyclerView>(R.id.list_rv)
        listTitleTV.text = title

        if (list.size > 0) {
            listRV.layoutManager = LinearLayoutManager(context)
            adapter = MemberListItemAdapter(context, list)
            listRV.adapter = adapter

            adapter!!.setOnClickListener(
                object : MemberListItemAdapter.OnClickListener {
                    override fun onClick(position: Int, user: User, action: String) {
                        dismiss()
                        onItemSelected(user, action)
                    }
                }
            )
        }
    }

    protected abstract fun onItemSelected(user: User, action: String)
}