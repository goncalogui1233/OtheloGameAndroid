package com.example.otello.game.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.otello.game.model.Posicoes
import com.example.otello.R
import com.example.otello.game.viewmodel.GameViewModel
import com.example.otello.game.adapter.RecyclerViewAdapter
import com.example.otello.game.model.Jogador
import kotlinx.android.synthetic.main.activity_game.*
import kotlin.random.Random

class GameActivity : AppCompatActivity(), RecyclerViewAdapter.ItemClickListener {

    lateinit var adapter : RecyclerViewAdapter
    lateinit var v : GameViewModel
    val boardD = 8
    var shouldSeeMoves : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        //Coloca o número de jogadores existentes
        //v.numJogadores.value = intent.getIntExtra("num_jogadores", 0)

        v = ViewModelProvider(this).get(GameViewModel::class.java)
        v.initBoard(boardD * boardD, boardD, 2)
        v.board.observe(this, observeBoard)
        v.playerTurn.observe(this, observePlayerTurn)
        v.playPositions.observe(this, observePlayerMoves)
        v.pontuacaoPlayers.observe(this, observePontuacoes)
        v.endGame.observe(this, observeEndGame)

        //Setting the Adapter
        adapter = RecyclerViewAdapter(this, boardD)
        adapter.setClickListener(this)

        //Set Board RecyclerView
        setRecyclerView()

        //Decidir quem joga primeiro
        sortearJogador()

        passTurnBtn.setOnClickListener {
            v.changePlayer()
        }

        showMovesBtn.setOnClickListener {
            shouldSeeMoves = !shouldSeeMoves
            if(shouldSeeMoves)
                adapter.setPlayerMoves(v.playPositions.value!!)
            else
                adapter.setPlayerMoves(arrayListOf())

            adapter.notifyDataSetChanged()
        }
    }

    private fun setRecyclerView(){
        recyclerView.layoutManager = GridLayoutManager(this, boardD)
        recyclerView.adapter = adapter
    }

    private fun sortearJogador(){
        val turn = Random.nextInt(0, v.numJogadores.value?.size!!) + 1
        v.changePlayer(turn)
    }

    private val observeBoard = Observer<Array<IntArray>> {
        adapter.setDataArray(it)
        adapter.notifyDataSetChanged()
    }

    private val observePlayerTurn = Observer<Jogador> {
        playerTurnInfo.text = resources.getString(R.string.player)
            .replace("[X]", v.playerTurn.value?.id.toString())
    }

    private val observeEndGame = Observer<Boolean> {
        if(it){
            //TODO - Show AlertDialog with who won and then, ok button to finish activity
            Toast.makeText(this, "O jogo acabou", Toast.LENGTH_LONG).show()
        }
    }

    private val observePlayerMoves = Observer<ArrayList<Posicoes>> {
        if(shouldSeeMoves) {
            adapter.setPlayerMoves(it)
            adapter.notifyDataSetChanged()
        }
    }

    private val observePontuacoes = Observer<ArrayList<Int>> {
        pontuacoesInfo.text = resources.getString(R.string.twoPlayerScore)
            .replace("[A]", it[0].toString())
            .replace("[B]", it[1].toString())
    }

    override fun onItemClick(view: View?, line: Int, column: Int) {
        v.updateValue(line, column)
    }
}