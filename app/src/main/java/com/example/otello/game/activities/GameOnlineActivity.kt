package com.example.otello.game.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.otello.R
import com.example.otello.game.adapter.GridAdapter
import com.example.otello.game.model.Jogador
import com.example.otello.game.model.Posicoes
import com.example.otello.game.viewmodel.GameViewModel
import com.example.otello.network.manager.NetworkManager
import com.example.otello.network.model.ConnType
import com.example.otello.utils.ConstStrings
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_game.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.random.Random

class GameOnlineActivity : AppCompatActivity() {

    lateinit var adapter : GridAdapter
    lateinit var v : GameViewModel
    var boardD = 8
    var shouldSeeMoves : Boolean = false
    var connType : ConnType? = null
    var gameMode : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        connType = ConnType.valueOf(intent.getStringExtra(ConstStrings.INTENT_CONN_TYPE)!!)
        gameMode = intent.getStringExtra(ConstStrings.INTENT_GAME_MODE)!!

        if(connType == ConnType.SERVER) {
            v = ViewModelProvider(this).get(GameViewModel::class.java)
            v.initBoard(boardD * boardD, boardD, 2)
            v.gameModel.board.observe(this, observeBoard)
            v.gameModel.playerTurn.observe(this, observePlayerTurn)
            v.gameModel.playPositions.observe(this, observePlayerMoves)
            v.gameModel.endGame.observe(this, observeEndGame)

            //Setting the Adapter, Dimensions and ClickListener for Grid
            setGridView(true)

            //Decidir quem joga primeiro
            sortearJogador()
        }
        else {
            receiveGameInfo()
            val json = JSONObject()
            json.put(ConstStrings.TYPE, "WantData")
            NetworkManager.sendInfo(NetworkManager.socketEnt!!, json.toString())
        }

        //TODO - Se jogo for local, configurar o array dos jogadores

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

        changePieceBtn.setOnClickListener {
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
    }

    private fun receiveGameInfo() {
        thread {
            while (true) {
                var str: String = ""
                try {
                    str = NetworkManager.receiveInfo(NetworkManager.socketEnt!!)
                } catch (e: Exception) {
                    return@thread
                }

                if (str != "") {
                    val json = JSONObject(str)
                    when (json.optString("Type")) {
                        "InitialInfo" -> {
                            boardD = json.optInt("boardDimension")
                            runOnUiThread {
                                setGridView(false)
                                val posArray = json.optJSONArray("positions")
                                for(pos in 0 until posArray.length()) {
                                    val posi = posArray.getJSONObject(pos)
                                    if(posi != null) {
                                        adapter.setPositionBoard(posi.getInt("linha"),posi.getInt("coluna"), posi.getInt("value"))
                                    }
                                }
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setGridView(isServer : Boolean) {
        if(isServer) {
            adapter = GridAdapter(this, v.gameModel.board.value!!)
            boardGrid.numColumns = v.gameModel.boardDimensions.value!!
        }
        else {
            adapter = GridAdapter(this, boardD)
            boardGrid.numColumns = boardD
        }
        boardGrid.adapter = adapter
        boardGrid.setOnItemClickListener { _, _, i, _ ->
            val linha = i / 8
            val coluna = i.rem(8)

            if(isServer) {
                if (v.gameModel.changePiecesMove.value!!) {
                    v.gameModel.changePieceArray.add(Posicoes(linha, coluna))
                    if (v.gameModel.changePieceArray.size == 3) {
                        v.changePieceMove()
                        v.gameModel.changePieceArray.clear()
                    }
                } else {
                    v.updateValue(linha, coluna)
                }
            }
            else {
                val json = JSONObject()
                json.put("linha", linha)
                json.put("coluna", coluna)
                NetworkManager.sendInfo(NetworkManager.socketEnt!!, json.toString())
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
        //Atualizar o ecrã sobre o atual jogador
        playerTurnInfo.text = resources.getString(R.string.player)
                .replace("[X]", v.gameModel.playerTurn.value?.id.toString())

        //Ativar/desativar botões para jogadas especiais
        bombBtn.isEnabled = it.bombPiece
        changePieceBtn.isEnabled = it.pieceChange

        //Ao mudar o jogador, atualizar as pontuações
        pontuacoesInfo.text = resources.getString(R.string.twoPlayerScore)
                .replace("[A]", v.gameModel.numJogadores.value!![0].score.toString())
                .replace("[B]", v.gameModel.numJogadores.value!![1].score.toString())
    }

    private val observeEndGame = Observer<Boolean> {
        if(it){
            if(v.gameModel.numJogadores.value != null) {
                var winner = v.gameModel.numJogadores.value!![0]
                for (i in 1 until v.gameModel.numJogadores.value?.size!!) {
                    if(v.gameModel.numJogadores.value!![i].score > winner.score){
                        winner = v.gameModel.numJogadores.value!![i]
                    }
                }

                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.endGame))
                        .setMessage(resources.getString(R.string.finalMessage)
                                .replace("[X]", winner.id.toString())
                                .replace("[Y]", winner.score.toString()))
                        .setCancelable(false)
                        .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                            finish()
                        }
                        .show()

            }
        }
    }

    private val observePlayerMoves = Observer<ArrayList<Posicoes>> {
        if(shouldSeeMoves) {
            adapter.setPlayerMoves(it)
            adapter.notifyDataSetChanged()
        }
    }

}