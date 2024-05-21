package com.example.projectmanager.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmanager.R
import com.example.projectmanager.activity.TaskActivity
import com.example.projectmanager.model.Task
import java.util.Collections

open class TaskListItemAdapter(
    private val context: Context,
    private var list: ArrayList<Task>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mPositionDraggedFrom = -1
    private var mPositionDraggedTo = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
        val layoutParams = LinearLayout.LayoutParams(
            (parent.width * 0.7).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins((15.toDp()).toPx(), 0, (40.toDp()).toPx(), 0)
        view.layoutParams = layoutParams
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder) {
            if (position == list.size - 1) {
                holder.itemView.findViewById<TextView?>(R.id.add_task_list_tv).visibility = View.VISIBLE
                holder.itemView.findViewById<LinearLayout?>(R.id.task_item_ll).visibility = View.GONE
            } else {
                holder.itemView.findViewById<TextView>(R.id.add_task_list_tv).visibility = View.GONE
                holder.itemView.findViewById<LinearLayout>(R.id.task_item_ll).visibility = View.VISIBLE
            }
            holder.itemView.findViewById<TextView>(R.id.task_title_tv).text = model.title

            holder.itemView.findViewById<TextView>(R.id.add_task_list_tv).setOnClickListener {
                holder.itemView.findViewById<TextView>(R.id.add_task_list_tv).visibility = View.GONE
                holder.itemView.findViewById<CardView>(R.id.add_task_name_cv).visibility = View.VISIBLE
            }
            holder.itemView.findViewById<ImageButton>(R.id.cancel_list_name_ib).setOnClickListener {
                holder.itemView.findViewById<TextView>(R.id.add_task_list_tv).visibility = View.VISIBLE
                holder.itemView.findViewById<CardView>(R.id.add_task_name_cv).visibility = View.GONE
            }
            holder.itemView.findViewById<ImageButton>(R.id.done_list_name_ib).setOnClickListener {
                val listName = holder.itemView.findViewById<EditText>(R.id.list_name_et).text.toString()
                if (listName.isNotEmpty()) {
                    if (context is TaskActivity) {
                        context.createTaskList(listName)
                    }
                }
                else {
                    Toast.makeText(context, "Please Enter List Name", Toast.LENGTH_SHORT).show()
                }
            }
            holder.itemView.findViewById<ImageButton>(R.id.edit_list_ib).setOnClickListener {
                holder.itemView.findViewById<EditText>(R.id.editable_list_name_et).setText(model.title)
                holder.itemView.findViewById<LinearLayout>(R.id.title_view_ll).visibility = View.GONE
                holder.itemView.findViewById<CardView>(R.id.edit_list_name_cv).visibility = View.VISIBLE
            }
            holder.itemView.findViewById<ImageButton>(R.id.cancel_editable_list_name_ib).setOnClickListener {
                holder.itemView.findViewById<LinearLayout>(R.id.title_view_ll).visibility = View.VISIBLE
                holder.itemView.findViewById<CardView>(R.id.edit_list_name_cv).visibility = View.GONE
            }
            holder.itemView.findViewById<ImageButton>(R.id.done_editable_list_name_ib).setOnClickListener {

                val listName = holder.itemView.findViewById<EditText>(R.id.editable_list_name_et).text.toString()
                if (listName.isNotEmpty()) {

                    if (context is TaskActivity) {
                        context.updateTaskList( position, listName, model)
                    }
                } else {
                    Toast.makeText(context, "Please Enter a List Name", Toast.LENGTH_SHORT).show()
                }
            }
            holder.itemView.findViewById<ImageButton>(R.id.delete_list_ib).setOnClickListener {
                alertDialogForDeleteList(position, model.title)
            }
            holder.itemView.findViewById<TextView>(R.id.add_card_tv).setOnClickListener {
                holder.itemView.findViewById<TextView>(R.id.add_card_tv).visibility = View.GONE
                holder.itemView.findViewById<CardView>(R.id.add_card_cv).visibility = View.VISIBLE
            }
            holder.itemView.findViewById<ImageButton>(R.id.cancel_card_name_ib).setOnClickListener {
                holder.itemView.findViewById<TextView>(R.id.add_card_tv).visibility = View.VISIBLE
                holder.itemView.findViewById<CardView>(R.id.add_card_cv).visibility = View.GONE
            }
            holder.itemView.findViewById<ImageButton>(R.id.done_card_name_ib).setOnClickListener {
                val cardName = holder.itemView.findViewById<EditText>(R.id.card_name_et).text.toString()
                if (cardName.isNotEmpty()) {
                    if (context is TaskActivity) {
                        context.addCardToTaskList(position, cardName)
                    }
                }
                else {
                    Toast.makeText(context, "Please Enter a Card Name", Toast.LENGTH_SHORT).show()
                }
            }
            val cardListRV = holder.itemView.findViewById<RecyclerView>(R.id.card_list_rv)
            cardListRV.layoutManager = LinearLayoutManager(context)
            cardListRV.setHasFixedSize(true)

            val adapter = CardListItemsAdapter(context, model.cards)
            cardListRV.adapter = adapter

            adapter.seOnClickListener(object: CardListItemsAdapter.OnClickListener{
                override fun onClick(position: Int) {
                    if (context is TaskActivity) {
                        context.cardDetails(holder.adapterPosition, position )
                    }
                }
            })
            val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            cardListRV.addItemDecoration(dividerItemDecoration)

            val helper = ItemTouchHelper( object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
            ){
                override fun onMove(
                    recyclerView: RecyclerView,
                    dragged: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val draggedPosition = dragged.adapterPosition
                    val targetPosition = target.adapterPosition

                    if (mPositionDraggedFrom == -1) {
                        mPositionDraggedFrom = draggedPosition
                    }
                    mPositionDraggedTo = targetPosition
                    Collections.swap(list[holder.adapterPosition].cards, draggedPosition, targetPosition)
                    adapter.notifyItemMoved(draggedPosition, targetPosition)
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    TODO("Not yet implemented")
                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {
                    super.clearView(recyclerView, viewHolder)
                    if (mPositionDraggedFrom != -1 && mPositionDraggedTo != -1 && mPositionDraggedFrom != mPositionDraggedTo) {
                        (context as TaskActivity).updateCardInTaskList(
                            holder.adapterPosition, list[holder.adapterPosition].cards
                        )
                    }
                    mPositionDraggedFrom = -1
                    mPositionDraggedTo = -1
                }
            })
            helper.attachToRecyclerView(cardListRV)
        }
    }

    private fun alertDialogForDeleteList(position: Int, title: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete $title?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes") {
            dialogInterface, which ->
            dialogInterface.dismiss()
            if (context is TaskActivity) {
                context.deleteTaskList(position)
            }
        }
        builder.setNegativeButton("No") {
            dialogInterface, which ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()
    private fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    class MyViewHolder(view: View): RecyclerView.ViewHolder(view)
}