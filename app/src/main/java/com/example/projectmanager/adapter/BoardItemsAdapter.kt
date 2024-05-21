package com.example.projectmanager.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.model.Board

open class BoardItemsAdapter(
    private val context: Context,
    private var list: ArrayList<Board>
) : RecyclerView.Adapter<ViewHolder>(){

    private var onClickListener: OnClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_board, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val board = list[position]

        if (holder is MyViewHolder) {
            Glide
                .with(context)
                .load(board.image.toUri().buildUpon().scheme("https").build())
                .centerCrop()
                .placeholder(R.drawable.ic_board_place_holder)
                .into(holder.itemView.findViewById(R.id.board_image_iv))

            holder.itemView.findViewById<TextView>(R.id.board_name_tv).text = board.name
            holder.itemView.findViewById<TextView>(R.id.created_by_tv).text = "Created by: ${board.createdBy}"

            holder.itemView.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick(position, board)
                }
            }
        }
    }

    interface OnClickListener{
        fun onClick(position: Int, board: Board)
    }

    fun seOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    private class MyViewHolder(view: View): ViewHolder(view)

}