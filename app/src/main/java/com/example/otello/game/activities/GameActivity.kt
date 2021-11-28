package com.example.otello.game.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.otello.game.model.Posicoes
import com.example.otello.R
import com.example.otello.game.adapter.GridAdapter
import com.example.otello.game.viewmodel.GameViewModel
import com.example.otello.game.model.Jogador
import kotlinx.android.synthetic.main.activity_game.*
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    lateinit var adapter : GridAdapter
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
        v.gameModel.board.observe(this, observeBoard)
        v.gameModel.playerTurn.observe(this, observePlayerTurn)
        v.gameModel.playPositions.observe(this, observePlayerMoves)
        v.gameModel.pontuacaoPlayers.observe(this, observePontuacoes)
        v.gameModel.endGame.observe(this, observeEndGame)

        //Setting the Adapter, Dimensions and ClickListener for Grid
        adapter = GridAdapter(this, v.gameModel.board.value!!)
        boardGrid.numColumns = v.gameModel.boardDimensions.value!!
        boardGrid.adapter = adapter
        boardGrid.setOnItemClickListener { _, _, i, _ ->
            val linha = i / 8
            val coluna = i.rem(8)

            if(v.gameModel.changePiecesMove){
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

        passTurnBtn.setOnClickListener {
            v.changePlayer()
        }

        showMovesBtn.setOnClickListener {
            shouldSeeMoves = !shouldSeeMoves
            if(shouldSeeMoves)
                adapter.setPlayerMoves(v.gameModel.playPositions.value!!)
            else
                adapter.setPlayerMoves(arrayListOf())

            adapter.notifyDataSetChanged()
        }

        bombBtn.setOnClickListener {
            v.gameModel.bombMove = true
        }

        changePieceBtn.setOnClickListener {
            if(!v.gameModel.changePiecesMove){
                v.gameModel.changePiecesMove = true
                it.setBackgroundColor(Color.BLACK)
            }
            else {
                v.gameModel.changePiecesMove = false
                v.gameModel.changePieceArray.clear()
                it.setBackgroundColor(Color.GREEN)
            }

        }
    }

    private fun sortearJogador(){
        val turn = Random.nextInt(0, v.gameModel.numJogadores.value?.size!!) + 1
        v.changePlayer(turn)
    }

    private val observeBoard = Observer<Array<IntArray>> {
        adapter.setBoardContent(it)
    }

    private val observePlayerTurn = Observer<Jogador> {
        playerTurnInfo.text = resources.getString(R.string.player)
            .replace("[X]", v.gameModel.playerTurn.value?.id.toString())

        //Ativar/desativar botões para jogadas especiais
        bombBtn.isEnabled = it.bombPiece
        changePieceBtn.isEnabled = it.pieceChange
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

}