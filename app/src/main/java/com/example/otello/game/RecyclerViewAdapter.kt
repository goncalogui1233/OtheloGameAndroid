package com.example.otello.game

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RotateDrawable
import android.nfc.cardemulation.CardEmulation
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.otello.Posicoes
import com.example.otello.R


class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private var mData: Array<IntArray>
    private var mMoves: ArrayList<Posicoes>? = null
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
        val context = holder.itemView.context

        //Coloca id do jogador no board
        if(mData[linha][coluna] != 0){
            holder.cardViewSquare.visibility = View.VISIBLE
            holder.playerId.text = mData[linha][coluna].toString()
            holder.squareLayout.background = ColorDrawable(Color.YELLOW)

            when(holder.playerId.text){
                "1" -> {
                    holder.cardViewSquare.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    holder.playerId.setTextColor(Color.BLACK)
                }
                "2" -> {
                    holder.cardViewSquare.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPlayer2Circle))
                    holder.playerId.setTextColor(Color.WHITE)
                }
                "3" -> {
                    holder.cardViewSquare.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPlayer3Circle))
                    holder.playerId.setTextColor(Color.BLACK)
                }
            }

        }

        //Altera as cores das posições onde o jogador atual pode jogar.
        if(mMoves != null && mMoves?.size!! > 0) {
            for (pos in mMoves!!) {
                if (pos.linha == linha && pos.coluna == coluna) {
                    holder.squareLayout.background = ContextCompat.getDrawable(holder.itemView.context, R.color.colorPossibleMove)
                }
            }
        }
    }

    // total number of cells
    override fun getItemCount(): Int {
        return mData.size
    }

    fun setDataArray(dataArray : Array<IntArray>){
        mData = dataArray
    }

    fun setPlayerMoves(array: ArrayList<Posicoes>){
        mMoves = array
    }

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val playerId = itemView.findViewById<TextView>(R.id.square_description)
        val squareLayout = itemView.findViewById<LinearLayout>(R.id.square_layout)
        val cardViewSquare = itemView.findViewById<CardView>(R.id.cardView_square)

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

    // Método para extrair dados do array do Adapter. Talvez possa ser necessário?
    fun getItem(id: Int): String {
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