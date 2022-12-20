package com.example.otello.game.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.otello.R
import com.example.otello.game.adapter.GridAdapter
import com.example.otello.utils.OtheloUtils
import kotlinx.android.synthetic.main.fragment_game.view.*

class GameOffline : GameBaseFragment() {

    companion object {
        fun newInstance(): GameOffline {
            val args = Bundle()
            val fragment = GameOffline()
            fragment.arguments = args
            return fragment
        }
    }

    override fun playerNumber(): Int = 2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

        v.initBoard()

        adapter = GridAdapter(requireContext(), playerNumber())
        view.boardGrid.numColumns = OtheloUtils.getBoardDimensionByPlayerNumber(playerNumber())
        view.boardGrid.adapter = adapter
        view.boardGrid.setOnItemClickListener { _, _, i, _ ->
            v.insertPieceOnBoard(i)
        }

        view.passTurnBtn.setOnClickListener {
            v.changePlayer()
        }

        //Decidir quem joga primeiro
        sortearJogador()

        return view
    }

}