package com.example.otello.game

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.otello.Posicoes
import com.example.otello.R
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
        val turn = Random.nextInt(0, v.numJogadores.value!!) + 1
        v.changePlayer(turn)
    }

    private val observeBoard = Observer<Array<IntArray>> {
        adapter.setDataArray(it)
        adapter.notifyDataSetChanged()
    }

    private val observePlayerTurn = Observer<Int> {
        playerTurnInfo.text = "Player " + v.playerTurn.value
    }

    private val observePlayerMoves = Observer<ArrayList<Posicoes>> {
        if(shouldSeeMoves) {
            adapter.setPlayerMoves(it)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onItemClick(view: View?, line: Int, column: Int) {
        v.updateValue(line, column)
    }
}