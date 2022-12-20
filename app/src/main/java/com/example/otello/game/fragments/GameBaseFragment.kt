package com.example.otello.game.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.otello.R
import com.example.otello.game.adapter.GridAdapter
import com.example.otello.game.model.*
import com.example.otello.game.repository.GameRepository
import com.example.otello.game.viewmodel.GameViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_game.*
import kotlin.random.Random

abstract class GameBaseFragment : Fragment() {

    lateinit var adapter : GridAdapter
    val v : GameViewModel by viewModels()
    var shouldSeeMoves : Boolean = false

    var isGameRunning = false

    abstract fun playerNumber() : Int

    private val observeBoard = Observer<Array<IntArray>> {
        adapter.setBoardContent(it)
    }

    private val observePlayerTurn = Observer<Jogador> {
        //Atualizar o ecrã sobre o atual jogador
        playerTurnInfo.text = resources.getString(R.string.player).replace("[X]", it.id.toString())
    }

    private val observeScoreChange = Observer<ArrayList<Int>> {
        //Atualizar as pontuações
        pontuacoesInfo.text = resources.getString(R.string.twoPlayerScore).replace("[A]", it[0].toString()).replace("[B]", it[1].toString())
    }

    private val observeGameStatus = Observer<GameStates> {
        when(it) {
            is GameStates.EndGame -> {
                it.winner?.let { winner ->
                    AlertDialog.Builder(requireContext())
                        .setTitle(resources.getString(R.string.endGame))
                        .setMessage(resources.getString(R.string.finalMessage)
                            .replace("[X]", winner.id.toString())
                            .replace("[Y]", winner.score.toString()))
                        .setCancelable(false)
                        .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                            requireActivity().finish()
                        }
                        .show()
                }
            }

            else -> {}
        }
    }

    private val observePlayerMoves = Observer<ArrayList<Posicoes>> {
        if(shouldSeeMoves) {
            adapter.setPlayerMoves(it)
        }

        if(it.size == 0) {
            v.setPlayerNoPossibleMoves()
            passTurnBtn.visibility = View.VISIBLE
        }
        else {
            passTurnBtn.visibility = View.GONE
        }
    }


    private val observePieceChangeMove = Observer<ArrayList<Posicoes>> {
        if(it.size == 3){
            v.changePieceMove()
            v.clearPieceChangeArray()
        }
    }

    private val observeBombActionChange = Observer<Boolean> {
        if(isGameRunning) {
            if (it) {
                Snackbar.make(gameLayout, resources.getString(R.string.bombSpecial) + " " + resources.getString(R.string.activated), Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(gameLayout, resources.getString(R.string.bombSpecial) + " " + resources.getString(R.string.deactivated), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private val observePieceChangeActionChange = Observer<Boolean> {
        if(isGameRunning) {
            if(it) {
                Snackbar.make(gameLayout, resources.getString(R.string.changePieceSpecial) + " " + resources.getString(R.string.activated), Snackbar.LENGTH_LONG).show()
            }
            else {
                v.clearPieceChangeArray()
                Snackbar.make(gameLayout, resources.getString(R.string.changePieceSpecial) + " " + resources.getString(R.string.deactivated), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private val observeSeeMovesState = Observer<Boolean> {
        shouldSeeMoves = it
        if(!it) {
            adapter.setPlayerMoves(arrayListOf())
        }
    }

    fun sortearJogador(){
        val turn = Random.nextInt(1, playerNumber() + 1)
        v.changePlayer(turn)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        v.getBoardLiveData().observe(viewLifecycleOwner, observeBoard)
        v.getBombActionStateLiveData().observe(viewLifecycleOwner, observeBombActionChange)
        v.getPieceChangeActionStateLiveData().observe(viewLifecycleOwner, observePieceChangeActionChange)
        v.getSeeMovesStateLiveData().observe(viewLifecycleOwner, observeSeeMovesState)
        v.getGameStateLiveData().observe(viewLifecycleOwner, observeGameStatus)
        v.getPlayerTurnStateLiveData().observe(viewLifecycleOwner, observePlayerTurn)
        v.getCurrPlayerPlayPositionsLiveData().observe(viewLifecycleOwner, observePlayerMoves)
        v.getScoresLiveData().observe(viewLifecycleOwner, observeScoreChange)
        v.getChangePieceArrayLiveData().observe(viewLifecycleOwner, observePieceChangeMove)
    }

    override fun onResume() {
        super.onResume()
        isGameRunning = true
    }

    override fun onPause() {
        super.onPause()
        isGameRunning = false
    }


}