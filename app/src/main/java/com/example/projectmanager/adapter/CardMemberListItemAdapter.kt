package com.example.projectmanager.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.model.SelectedMember

open class CardMemberListItemAdapter(
    private val context: Context,
    private val list: ArrayList<SelectedMember>,
    private val assignMembers: Boolean

): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_card_selected_member, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder) {
            if (position == list.size - 1 && assignMembers) {
                holder.itemView.findViewById<ImageView>(R.id.add_member_iv).visibility = View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.selected_member_image_iv).visibility = View.GONE
            } else {
                holder.itemView.findViewById<ImageView>(R.id.add_member_iv).visibility = View.GONE
                holder.itemView.findViewById<ImageView>(R.id.selected_member_image_iv).visibility = View.VISIBLE
                Glide
                    .with(context)
                    .load(model.image.toUri())
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(holder.itemView.findViewById(R.id.selected_member_image_iv))
            }
            holder.itemView.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick()
                }
            }
        }
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    interface OnClickListener{
        fun onClick()
    }

    class MyViewHolder(view: View): RecyclerView.ViewHolder(view)
}