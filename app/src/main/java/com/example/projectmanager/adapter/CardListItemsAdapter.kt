package com.example.projectmanager.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmanager.R
import com.example.projectmanager.activity.TaskActivity
import com.example.projectmanager.model.Card
import com.example.projectmanager.model.SelectedMember

class CardListItemsAdapter(
    private val context: Context,
    private val list: ArrayList<Card>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_card, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        val colorLabelView = holder.itemView.findViewById<View>(R.id.color_label_view)

        if (holder is MyViewHolder) {
            if (model.labelColor.isNotEmpty()) {
                colorLabelView.visibility = View.VISIBLE
                colorLabelView.setBackgroundColor(Color.parseColor(model.labelColor))
            } else {
                colorLabelView.visibility = View.GONE
            }
            holder.itemView.findViewById<TextView>(R.id.card_name_tv).text = model.name
            if ((context as TaskActivity).mAssignedMemberDetails.size > 0) {
                val selectedMemberList: ArrayList<SelectedMember> = ArrayList()
                for (i in context.mAssignedMemberDetails.indices) {
                    for (j in model.assignedTo) {
                        if (context.mAssignedMemberDetails[i].id == j) {
                            val selectedMember = SelectedMember(
                                context.mAssignedMemberDetails[i].id,
                                context.mAssignedMemberDetails[i].image
                            )
                            selectedMemberList.add(selectedMember)
                        }
                    }
                }
                if (selectedMemberList.size > 0) {
                    val cardSelectedMemberRV = holder.itemView.findViewById<RecyclerView>(R.id.card_selected_members_RV)
                    if (selectedMemberList.size == 1 && selectedMemberList[0].id == model.createdBy)
                        cardSelectedMemberRV.visibility = View.GONE
                    else {
                        cardSelectedMemberRV.visibility = View.VISIBLE
                        cardSelectedMemberRV.layoutManager = GridLayoutManager(context, 4)
                        val adapter = CardMemberListItemAdapter(context, selectedMemberList, false)
                        cardSelectedMemberRV.adapter = adapter
                        adapter.setOnClickListener(
                            object : CardMemberListItemAdapter.OnClickListener {
                                override fun onClick() {
                                    if (onClickListener != null) {
                                        onClickListener!!.onClick(holder.adapterPosition)
                                    }
                                }
                            }
                        )
                    }
                } else {
                    holder.itemView.findViewById<RecyclerView>(R.id.card_selected_members_RV).visibility = View.GONE
                }
            }
            holder.itemView.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick(position)
                }
            }
        }
    }

    interface OnClickListener{
        fun onClick(position: Int)
    }

    fun seOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    private class MyViewHolder(view: View): RecyclerView.ViewHolder(view)

}