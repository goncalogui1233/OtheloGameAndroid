package com.example.otello.game.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import com.example.otello.R
import com.example.otello.game.model.Posicoes
import com.example.otello.utils.OtheloUtils
import kotlinx.android.synthetic.main.square_view.view.*


class GridAdapter : BaseAdapter {
    private var context: Context? = null
    private var gridContent: ArrayList<Int>? = null
    private var playerMoves : ArrayList<Posicoes>? = null
    private var mBoardSize : Int = 0

    constructor(c: Context?, content: Array<IntArray>, boardSize: Int) {
        context = c
        mBoardSize = boardSize
        fillArray(content)
    }

    constructor(context : Context, playerNumber : Int) {
        this.context = context
        this.mBoardSize = OtheloUtils.getBoardDimensionByPlayerNumber(playerNumber)
        fillArray(Array(mBoardSize) { IntArray(mBoardSize) })
    }

    fun setBoardContent(array: Array<IntArray>){
        fillArray(array)
        notifyDataSetChanged()
    }

    fun setPositionBoard(line : Int, column : Int, value : Int) {
        gridContent!![line*column] = value
    }

    fun setPlayerMoves(array: ArrayList<Posicoes>){
        this.playerMoves = array
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return gridContent?.size!!
    }

    override fun getItem(p0: Int): Any {
        return p0
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    private fun fillArray(array: Array<IntArray>) {
        gridContent = ArrayList()

        array.forEach { col ->
            col.forEach { value ->
                gridContent?.add(value)
            }
        }
    }

    override fun getView(position: Int, view: View?, group: ViewGroup?): View? {

        val v = LayoutInflater.from(context).inflate(R.layout.square_view, group, false)

        val displayMetrics = context!!.resources.displayMetrics
        val squareDim = displayMetrics.widthPixels / mBoardSize

        val layoutParams = ViewGroup.LayoutParams(squareDim, squareDim)
        v.layoutParams = layoutParams

        if(gridContent!![position] != 0) {
            v.cardView_square.visibility = View.VISIBLE
            v.square_description.text = gridContent!![position].toString()

            when (gridContent!![position]) {
                1 -> {
                    v.cardView_square.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.white))
                    v.square_description.setTextColor(Color.BLACK)
                }
                2 -> {
                    v.cardView_square.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPlayer2Circle))
                    v.square_description.setTextColor(Color.WHITE)
                }
                3 -> {
                    v.cardView_square.setCardBackgroundColor(ContextCompat.getColor(context!!, R.color.colorPlayer3Circle))
                    v.square_description.setTextColor(Color.BLACK)
                }
            }
        }

        //Altera as cores das posições onde o jogador atual pode jogar.
        if (playerMoves != null && playerMoves?.size!! > 0) {
            for (pos in playerMoves!!) {
                if (((pos.linha * mBoardSize) + pos.coluna) == position) {
                    v.square_layout.background = ContextCompat.getDrawable(context!!, R.color.colorPossibleMove)
                }
            }
        }

        return v
    }
}