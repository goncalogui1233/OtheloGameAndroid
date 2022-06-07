package com.example.otello.game.fragments

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.otello.R
import com.example.otello.game.adapter.GridAdapter
import com.example.otello.game.model.EndGameStates
import com.example.otello.game.model.Jogador
import com.example.otello.game.model.Posicoes
import com.example.otello.game.repository.GameRepository
import com.example.otello.game.viewmodel.GameViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_game.*
import kotlin.random.Random

abstract class GameBaseFragment : Fragment() {

    lateinit var adapter : GridAdapter
    val v : GameViewModel by viewModels()
    var shouldSeeMoves : Boolean = false

    val observeBoard = Observer<Array<IntArray>> {
        adapter.setBoardContent(it)
    }

    val observePlayerTurn = Observer<Jogador> {
        //Atualizar o ecrã sobre o atual jogador
        playerTurnInfo.text = resources.getString(R.string.player)
            .replace("[X]", v.gameModel.playerTurn.value?.id.toString())
    }

    val observeScoreChange = Observer<ArrayList<Int>> {
        //Atualizar as pontuações
        pontuacoesInfo.text = resources.getString(R.string.twoPlayerScore)
            .replace("[A]", it[0].toString())
            .replace("[B]", it[1].toString())
    }

    val observeGameStatus = Observer<EndGameStates> {
        if(it == EndGameStates.FINISHED){
            if(v.gameModel.numJogadores.value != null) {
                var winner = v.gameModel.numJogadores.value!![0]
                for (i in 1 until v.gameModel.numJogadores.value?.size!!) {
                    if(v.gameModel.numJogadores.value!![i].score > winner.score){
                        winner = v.gameModel.numJogadores.value!![i]
                    }
                }
            }
        }
    }

    val observePlayerWinner = Observer<Jogador> {
        AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.endGame))
            .setMessage(resources.getString(R.string.finalMessage)
                .replace("[X]", it.id.toString())
                .replace("[Y]", it.score.toString()))
            .setCancelable(false)
            .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                requireActivity().finish()
            }
            .show()
    }

    val observePlayerMoves = Observer<ArrayList<Posicoes>> {
        if(shouldSeeMoves) {
            adapter.setPlayerMoves(it)
            adapter.notifyDataSetChanged()
        }

        if(it.size == 0) {
            v.gameModel.playerTurn.value!!.hadMoves = false
            passTurnBtn.visibility = View.VISIBLE
        }
        else {
            passTurnBtn.visibility = View.GONE
        }

        GameRepository.checkPlayerMoves()
    }

    fun sortearJogador(){
        val turn = Random.nextInt(0, v.gameModel.numJogadores.value?.size!!) + 1
        v.changePlayer(turn)
    }

    private fun bombAction() {
        //Só ativa o special da bomba caso outro special não esteja ativo
        if(!v.gameModel.changePiecesMove.value!!) {
            if (v.gameModel.bombMove.value!!) {
                v.gameModel.bombMove.value = false
                Snackbar.make(gameLayout,
                    resources.getString(R.string.bombSpecial) + " " + resources.getString(R.string.deactivated),
                    Snackbar.LENGTH_LONG).show()
            }
            else {
                v.gameModel.bombMove.value = true
                Snackbar.make(gameLayout,
                    resources.getString(R.string.bombSpecial) + " " + resources.getString(R.string.activated),
                    Snackbar.LENGTH_LONG).show()
            }
        }
        else {
            Snackbar.make(gameLayout,
                resources.getString(R.string.noBombPossible), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun pieceAction() {
        if(!v.gameModel.bombMove.value!!) {
            if (!v.gameModel.changePiecesMove.value!!) {
                v.gameModel.changePiecesMove.value = true
                Snackbar.make(gameLayout,
                    resources.getString(R.string.changePieceSpecial) + " " + resources.getString(R.string.activated),
                    Snackbar.LENGTH_LONG).show()
            } else {
                v.gameModel.changePiecesMove.value = false
                v.gameModel.changePieceArray.clear()
                Snackbar.make(gameLayout,
                    resources.getString(R.string.changePieceSpecial) + " " + resources.getString(R.string.deactivated),
                    Snackbar.LENGTH_LONG).show()
            }
        }
        else {
            Snackbar.make(gameLayout,
                resources.getString(R.string.noChangePiecePossible), Snackbar.LENGTH_LONG).show()
        }
    }


    fun menuClicked(itemId : Int) {
        when(itemId) {

            R.id.showMoves -> {
                shouldSeeMoves = !shouldSeeMoves
                if(shouldSeeMoves)
                    adapter.setPlayerMoves(v.gameModel.playPositions.value!!)
                else
                    adapter.setPlayerMoves(arrayListOf())

                adapter.notifyDataSetChanged()
            }

            R.id.bombTrigger -> bombAction()

            R.id.pieceTrigger -> pieceAction()

            R.id.updateInfos -> {}
        }
    }


}