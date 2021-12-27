package com.example.otello.scores.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.otello.R
import com.example.otello.scores.model.ScoresClass
import org.w3c.dom.Text


class ScoresAdapter : RecyclerView.Adapter<ScoresAdapter.ViewHolder>{
    private var mScores: List<ScoresClass>? = null
    private var mInflater: LayoutInflater? = null
    private lateinit var mContext : Context

    // data is passed into the constructor
    constructor(context: Context, data: List<ScoresClass>) {
        mInflater = LayoutInflater.from(context)
        mScores = data
        mContext = context
    }

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = mInflater?.inflate(R.layout.scores_view, parent, false)!!
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val score = mScores!![position]
        holder.titleTextView.text = mContext.resources.getString(R.string.scoreTitle).replace("[X]",(position + 1).toString())
        holder.nameTextView.text = score.playerName
        holder.scoreTextView.text = score.playerScore.toString()
        holder.opponentTextView.text = score.opponentNumber.toString()
        holder.piecesTextView.text = score.piecesPlaced.toString()
    }

    // total number of rows
    override fun getItemCount(): Int {
        return mScores!!.size
    }

    // stores and recycles views as they are scrolled off screen
    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView : TextView
        val nameTextView: TextView
        val scoreTextView: TextView
        val opponentTextView: TextView
        val piecesTextView: TextView


        init {
            titleTextView = itemView.findViewById(R.id.scoreTitle)
            nameTextView = itemView.findViewById(R.id.scorePlayerName)
            scoreTextView = itemView.findViewById(R.id.scorePlayerScore)
            opponentTextView = itemView.findViewById(R.id.scoreOpponentNumber)
            piecesTextView = itemView.findViewById(R.id.scorePiecesPlaced)
        }
    }

}