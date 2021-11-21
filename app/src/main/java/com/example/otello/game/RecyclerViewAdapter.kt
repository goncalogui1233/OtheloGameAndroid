package com.example.otello.game

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.otello.R


class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private var mData: Array<IntArray>
    private var mBoardDimensions : Int = -1
    private var mInflater: LayoutInflater? = null
    private var mClickListener: ItemClickListener? = null

    // data is passed into the constructor
    constructor(context: Context?, boardDimensions : Int) {
        mInflater = LayoutInflater.from(context)
        mBoardDimensions = boardDimensions
        mData = Array(boardDimensions) { IntArray(boardDimensions) { 0 } }
    }

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater!!.inflate(R.layout.square_view, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each cell
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val linha = position.div(mBoardDimensions)
        val coluna = position.rem(mBoardDimensions)

        holder.myTextView.text = if(mData[linha][coluna] != 0) mData[linha][coluna].toString() else ""
    }

    // total number of cells
    override fun getItemCount(): Int {
        return mData.size
    }


    fun setDataArray(dataArray : Array<IntArray>){
        mData = dataArray
    }

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var myTextView: TextView = itemView.findViewById(R.id.square_description)

        override fun onClick(view: View) {
            if (mClickListener != null) {

                val line = absoluteAdapterPosition.div(8)
                val column = absoluteAdapterPosition.rem(8)

                mClickListener!!.onItemClick(view, line, column)
            }
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): String? {
        return mData[id].toString()
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        mClickListener = itemClickListener
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, line : Int, column : Int)
    }

}