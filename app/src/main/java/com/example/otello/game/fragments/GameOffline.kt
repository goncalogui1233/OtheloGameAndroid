package com.example.otello.game.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.otello.R
import com.example.otello.game.adapter.GridAdapter
import com.example.otello.game.model.Posicoes
import kotlinx.android.synthetic.main.fragment_game.view.*

class GameOffline : GameBaseFragment() {
    val boardD = 8

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

        v.initBoard(boardD * boardD, boardD, 2)
        v.gameModel.board.observe(viewLifecycleOwner, observeBoard)
        v.gameModel.playerTurn.observe(viewLifecycleOwner, observePlayerTurn)
        v.gameModel.playPositions.observe(viewLifecycleOwner, observePlayerMoves)
        v.gameModel.endGame.observe(viewLifecycleOwner, observeEndGame)

        adapter = GridAdapter(requireContext(), v.gameModel.board.value!!, v.gameModel.boardDimensions.value!!)
        //boardGrid.numColumns = v.gameModel.boardDimensions.value!!
        view.boardGrid.numColumns = v.gameModel.boardDimensions.value!!
        view.boardGrid.adapter = adapter
        view.boardGrid.setOnItemClickListener { _, _, i, _ ->
            val linha = i / v.gameModel.boardDimensions.value!!
            val coluna = i.rem(v.gameModel.boardDimensions.value!!)

            if(v.gameModel.changePiecesMove.value!!){
                v.gameModel.changePieceArray.add(Posicoes(linha, coluna))
                if(v.gameModel.changePieceArray.size == 3){
                    v.changePieceMove()
                    v.gameModel.changePieceArray.clear()
                }
            }
            else {
                v.updateValue(linha, coluna)
            }
        }

        //Decidir quem joga primeiro
        sortearJogador()

        view.passTurnBtn.setOnClickListener {
            v.changePlayer()
        }

        return view
    }


}