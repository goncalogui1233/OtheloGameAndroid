package com.example.otello.game.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.otello.R
import com.example.otello.game.adapter.GridAdapter
import com.example.otello.game.model.Jogador
import com.example.otello.game.model.Posicoes
import com.example.otello.game.viewmodel.GameOnlineViewModel
import com.example.otello.network.manager.NetworkManager
import com.example.otello.network.model.ConnType
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.OtheloUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_game.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import kotlin.concurrent.thread
import kotlin.random.Random

class GameOnlineActivity : AppCompatActivity() {

    lateinit var adapter : GridAdapter
    lateinit var v : GameOnlineViewModel
    var boardD = 8
    var shouldSeeMoves : Boolean = false
    var connType : ConnType? = null
    var gameMode : String = ""
    var currPlayerId : Int = -1
    var changePieceActivated = false
    var changePieceArray = arrayListOf<Posicoes>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        connType = ConnType.valueOf(intent.getStringExtra(ConstStrings.INTENT_CONN_TYPE)!!)
        gameMode = intent.getStringExtra(ConstStrings.INTENT_GAME_MODE)!!

        if(connType == ConnType.SERVER) {
            v = ViewModelProvider(this).get(GameOnlineViewModel::class.java)
            v.initBoard(boardD * boardD, boardD, 2)
            v.gameModel.board.observe(this, observeBoard)
            v.gameModel.playerTurn.observe(this, observePlayerTurn)
            v.gameModel.playPositions.observe(this, observePlayerMoves)
            v.gameModel.endGame.observe(this, observeEndGame)

            //Setting the Adapter, Dimensions and ClickListener for Grid
            setGridView(true)

            //Decidir quem joga primeiro
            sortearJogador()

            v.initComunication()
        }
        else {
            receiveGameInfo()
        }

        passTurnBtn.setOnClickListener {
            when(connType) {
                ConnType.SERVER -> v.checkNextPlayer()
                ConnType.CLIENT -> {
                    val json = JSONObject()
                    json.put(ConstStrings.TYPE, ConstStrings.GAME_PASS_TURN)
                    NetworkManager.sendInfo(NetworkManager.socketEnt!!, json.toString())
                }
            }
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
            when(connType) {
                ConnType.SERVER -> {
                    if(v.gameModel.playerTurn.value!!.id == NetworkManager.playerId) {
                        if (!v.gameModel.changePiecesMove.value!!) {
                            if (v.gameModel.bombMove.value!!) {
                                v.gameModel.bombMove.value = false
                                Snackbar.make(gameLayout,
                                        resources.getString(R.string.bombSpecial) + " " + resources.getString(R.string.deactivated),
                                        Snackbar.LENGTH_LONG).show()
                            } else {
                                v.gameModel.bombMove.value = true
                                Snackbar.make(gameLayout,
                                        resources.getString(R.string.bombSpecial) + " " + resources.getString(R.string.activated),
                                        Snackbar.LENGTH_LONG).show()
                            }
                        } else {
                            Snackbar.make(gameLayout,
                                    resources.getString(R.string.noBombPossible), Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
                ConnType.CLIENT -> {
                    if(currPlayerId == NetworkManager.playerId) {
                        val json = JSONObject()
                        json.put(ConstStrings.TYPE, ConstStrings.GAME_BOMB_MOVE_ON)
                        NetworkManager.sendInfo(NetworkManager.socketEnt!!, json.toString())
                    }
                }
            }

        }

        changePieceBtn.setOnClickListener {
            when(connType) {
                ConnType.SERVER -> {
                    if(v.gameModel.playerTurn.value!!.id == NetworkManager.playerId) {
                        if (!v.gameModel.bombMove.value!!) {
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
                        } else {
                            Snackbar.make(gameLayout,
                                    resources.getString(R.string.noChangePiecePossible), Snackbar.LENGTH_LONG).show()
                        }
                    }
                }

                ConnType.CLIENT -> {
                    if(currPlayerId == NetworkManager.playerId) {
                        val json = JSONObject()
                        json.put(ConstStrings.TYPE, ConstStrings.GAME_PIECE_MOVE_ON)
                        NetworkManager.sendInfo(NetworkManager.socketEnt!!, json.toString())
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
                if (v.gameModel.playerTurn.value!!.id == NetworkManager.playerId) {
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
            }
            else {
                if (currPlayerId == NetworkManager.playerId) {
                    val json = JSONObject()
                    json.put(ConstStrings.TYPE, ConstStrings.GAME_PLACED_PIECE)
                    json.put(ConstStrings.GAME_PIECE_POSITION, JSONObject()
                            .put(ConstStrings.BOARD_LINE, linha)
                            .put(ConstStrings.BOARD_COLUMN, coluna))

                    NetworkManager.sendInfo(NetworkManager.socketEnt!!, json.toString())
                }
            }
        }
    }

    private fun sortearJogador(){
        val turn = Random.nextInt(0, v.gameModel.numJogadores.value?.size!!) + 1
        v.gameModel.playerTurn.value = v.checkNextPlayer(turn)
    }

    private val observeBoard = Observer<Array<IntArray>> {
        adapter.setBoardContent(it)
    }

    private val observePlayerTurn = Observer<Jogador> {
        //Check where the player can play
        v.getPossiblePositions()

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


    /**
     * Function that receives information from server
     */
    private fun receiveGameInfo() {
        thread {
            val json = JSONObject()
            json.put(ConstStrings.TYPE, ConstStrings.CLIENT_WANT_DATA)
            NetworkManager.sendInfo(NetworkManager.socketEnt!!, json.toString())

            while (true) {
                var str: String = ""
                try {
                    str = BufferedReader(InputStreamReader(NetworkManager.socketEnt!!.getInputStream())).readLine()
                } catch (e: Exception) {
                    return@thread
                }

                if (str != "") {
                    val json = JSONObject(str)
                    when (json.optString(ConstStrings.TYPE)) {
                        ConstStrings.GAME_INIT_INFOS -> {
                            boardD = json.optInt(ConstStrings.GAME_BOARD_DIMENSION)
                            val scores = json.optJSONArray(ConstStrings.PLAYERS_SCORES)
                            val currPlayer = json.optJSONObject(ConstStrings.CURRENT_PLAYER)
                            currPlayerId = currPlayer.optInt(ConstStrings.PLAYER_ID)
                            runOnUiThread {
                                setGridView(false)
                                val posArray = json.optJSONArray(ConstStrings.BOARD_INIT_POSITIONS)
                                for (pos in 0 until posArray.length()) {
                                    val posi = posArray.getJSONObject(pos)
                                    if (posi != null) {
                                        adapter.setPositionBoard(posi.getInt(ConstStrings.BOARD_LINE),
                                                posi.getInt(ConstStrings.BOARD_COLUMN),
                                                posi.getInt(ConstStrings.BOARD_POS_VALUE))
                                    }
                                }

                                if (scores.length() == 2) {
                                    val p1 = scores.getJSONObject(0)
                                    val p2 = scores.getJSONObject(1)
                                    pontuacoesInfo.text = resources.getString(R.string.twoPlayerScoree)
                                            .replace("[1]", p1.optString(ConstStrings.PLAYER_NAME) ?: p1.optInt(ConstStrings.PLAYER_ID).toString())
                                            .replace("[A]", p1.optString(ConstStrings.PLAYER_SCORE))
                                            .replace("[2]", p2.optString(ConstStrings.PLAYER_NAME) ?: p2.optInt(ConstStrings.PLAYER_ID).toString())
                                            .replace("[B]", p2.optString(ConstStrings.PLAYER_SCORE))
                                }

                                playerTurnInfo.text = "Jogador: " + currPlayerId.toString() + "\nNome: " + currPlayer.optString(ConstStrings.PLAYER_NAME)
                                if (!currPlayer.optString(ConstStrings.PLAYER_PHOTO).isNullOrEmpty()) {
                                    playerImageView.visibility = View.VISIBLE
                                    playerImageView.setImageBitmap(OtheloUtils.getBitmapFromString(currPlayer.optString(ConstStrings.PLAYER_PHOTO)))
                                }
                            }
                        }

                        ConstStrings.GAME_BOMB_MOVE_ANSWER -> {
                            when (json.optString(ConstStrings.STATUS)) {
                                ConstStrings.GAME_BOMB_MOVE_ACTIVATED -> {
                                    Snackbar.make(gameLayout, resources.getString(R.string.bombSpecial) + " " + resources.getString(R.string.activated),
                                            Snackbar.LENGTH_LONG).show()
                                }

                                ConstStrings.GAME_BOMB_MOVE_DEACTIVATED -> {
                                    Snackbar.make(gameLayout, resources.getString(R.string.bombSpecial) + " " + resources.getString(R.string.deactivated),
                                            Snackbar.LENGTH_LONG).show()
                                }

                                ConstStrings.GAME_PIECE_MOVE_IS_ACTIVATED ->  Snackbar.make(gameLayout,
                                        resources.getString(R.string.noBombPossible), Snackbar.LENGTH_LONG).show()

                                ConstStrings.GAME_BOMB_MOVE_WAS_ACTIVATED -> {
                                    Snackbar.make(gameLayout,
                                            resources.getString(R.string.noBombMoveAgain), Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }

                        ConstStrings.GAME_PIECE_MOVE_ANSWER -> {
                            when (json.optString(ConstStrings.STATUS)) {
                                ConstStrings.GAME_PIECE_MOVE_ACTIVATED -> {
                                    Snackbar.make(gameLayout, resources.getString(R.string.changePieceSpecial) + " " + resources.getString(R.string.activated),
                                            Snackbar.LENGTH_LONG).show()
                                }

                                ConstStrings.GAME_PIECE_MOVE_DEACTIVATED -> {
                                    Snackbar.make(gameLayout, resources.getString(R.string.changePieceSpecial) + " " + resources.getString(R.string.deactivated),
                                            Snackbar.LENGTH_LONG).show()
                                }

                                ConstStrings.GAME_BOMB_MOVE_IS_ACTIVATED -> Snackbar.make(gameLayout,
                                        resources.getString(R.string.noChangePiecePossible), Snackbar.LENGTH_LONG).show()

                                ConstStrings.GAME_PIECE_MOVE_WAS_ACTIVATED -> Snackbar.make(gameLayout,
                                        resources.getString(R.string.noPieceMoveAgain), Snackbar.LENGTH_LONG).show()
                            }
                        }

                        ConstStrings.GAME_PUT_NEW_PIECE -> {
                            val newPos = json.optJSONArray(ConstStrings.GAME_NEW_POSITIONS)
                            val currPlayer = json.optJSONObject(ConstStrings.GAME_PASS_TURN)
                            val scores = json.optJSONArray(ConstStrings.PLAYERS_SCORES)
                            currPlayerId = currPlayer.optInt(ConstStrings.PLAYER_ID)

                            for(i in 0 until newPos.length()){
                                adapter.setPositionBoard(newPos.optJSONObject(i).optInt(ConstStrings.BOARD_LINE),
                                        newPos.optJSONObject(i).optInt(ConstStrings.BOARD_COLUMN),
                                        newPos.optJSONObject(i).optInt(ConstStrings.BOARD_POS_VALUE))
                            }

                            if(json.optBoolean(ConstStrings.GAME_VALID_PIECE) && newPos != null) {
                                runOnUiThread {
                                    adapter.notifyDataSetChanged()

                                    if (scores.length() == 2) {
                                        val p1 = scores.getJSONObject(0)
                                        val p2 = scores.getJSONObject(1)
                                        pontuacoesInfo.text = resources.getString(R.string.twoPlayerScoree)
                                                .replace("[1]", p1.optString(ConstStrings.PLAYER_NAME) ?: p1.optInt(ConstStrings.PLAYER_ID).toString())
                                                .replace("[A]", p1.optString(ConstStrings.PLAYER_SCORE))
                                                .replace("[2]", p2.optString(ConstStrings.PLAYER_NAME) ?: p2.optInt(ConstStrings.PLAYER_ID).toString())
                                                .replace("[B]", p2.optString(ConstStrings.PLAYER_SCORE))
                                    }

                                    playerTurnInfo.text = "Jogador: " + currPlayer.optInt(ConstStrings.PLAYER_ID).toString() + "\nNome: " + currPlayer.optString(ConstStrings.PLAYER_NAME)
                                    if (!currPlayer.optString(ConstStrings.PLAYER_PHOTO).isNullOrEmpty()) {
                                        playerImageView.visibility = View.VISIBLE
                                        playerImageView.setImageBitmap(OtheloUtils.getBitmapFromString(currPlayer.optString(ConstStrings.PLAYER_PHOTO)))
                                    }
                                    else {
                                        playerImageView.visibility = View.GONE
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}