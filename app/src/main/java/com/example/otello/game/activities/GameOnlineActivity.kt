package com.example.otello.game.activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.otello.R
import com.example.otello.game.adapter.GridAdapter
import com.example.otello.game.repository.GameRepository
import com.example.otello.game.model.Jogador
import com.example.otello.game.model.Posicoes
import com.example.otello.game.viewmodel.GameOnlineViewModel
import com.example.otello.network.manager.LobbyManager
import com.example.otello.network.model.ConnType
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.FirestoreUtils
import com.example.otello.utils.NetworkUtils
import com.example.otello.utils.OtheloUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_game.*
import org.json.JSONException
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
    var gameRunning = true
    var winnerObsTriggered = false
    var myPlayerId : Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_game)

        if(connType == ConnType.SERVER) {
            v = ViewModelProvider(this).get(GameOnlineViewModel::class.java)
            v.initBoard()
            v.gameModel.board.observe(this, observeBoard)
            v.gameModel.playerTurn.observe(this, observePlayerTurn)
            v.gameModel.playPositions.observe(this, observePlayerMoves)
            //v.gameModel.endGame.observe(this, observeEndGame)
            v.gameModel.playerWinner.observe(this, observeWinner)

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
                ConnType.SERVER -> {
                    v.passTurn(myPlayerId)
                }
                ConnType.CLIENT -> {
                    if(currPlayerId == myPlayerId) {
                        val json = JSONObject()
                        json.put(ConstStrings.TYPE, ConstStrings.GAME_PASS_TURN)
                        NetworkUtils.sendInfo(LobbyManager.gameSocket!!, json.toString())
                    }
                }
            }
        }
    }

    private fun setGridView(isServer : Boolean) {
        if(isServer) {
            adapter = GridAdapter(this, v.gameModel.board.value!!, v.gameModel.boardDimensions.value!!)
            boardGrid.numColumns = v.gameModel.boardDimensions.value!!
        }
        else {
            adapter = GridAdapter(this, boardD)
            boardGrid.numColumns = boardD
        }
        boardGrid.adapter = adapter
        boardGrid.setOnItemClickListener { _, _, i, _ ->
            if(isServer) {
                val linha = i / v.gameModel.boardDimensions.value!!
                val coluna = i.rem(v.gameModel.boardDimensions.value!!)
                if (v.gameModel.playerTurn.value!!.id == myPlayerId) {
                    if (v.gameModel.changePiecesMove.value!!) {
                        v.gameModel.changePieceArray.value?.add(Posicoes(linha, coluna))
                    } else {
                        v.updateValue(linha, coluna)
                    }
                }
            }
            else {
                val linha = i / boardD
                val coluna = i.rem(boardD)
                if (currPlayerId == myPlayerId) {
                    val json = JSONObject()
                    json.put(ConstStrings.TYPE, ConstStrings.GAME_PLACED_PIECE)
                    json.put(ConstStrings.GAME_PIECE_POSITION, JSONObject()
                            .put(ConstStrings.BOARD_LINE, linha)
                            .put(ConstStrings.BOARD_COLUMN, coluna))

                    NetworkUtils.sendInfo(LobbyManager.gameSocket!!, json.toString())
                }
            }
        }
    }

    private fun sortearJogador(){
        val turn = Random.nextInt(0, v.gameModel.numJogadores.value?.size!!) + 1
        val nextPlayer = v.checkNextPlayer(turn)
        v.gameModel.playerTurn.value = nextPlayer
        v.gameModel.playPositions.value = v.getPossiblePositions(nextPlayer)
    }

    private val observeBoard = Observer<Array<IntArray>> {
        adapter.setBoardContent(it)
    }

    private val observePlayerTurn = Observer<Jogador> {
        //Atualizar o ecrã sobre o atual jogador

        playerTurnInfo.text = resources.getString(R.string.player).replace("[X]", v.gameModel.playerTurn.value?.id.toString())
                .plus("\n").plus(resources.getString(R.string.moreInfoPlayer).replace("[Y]", v.gameModel.playerTurn.value?.name.toString()))

        //Ao mudar o jogador, atualizar as pontuações
        if(v.gameModel.numJogadores.value!!.size == 2) {
            pontuacoesInfo.text = resources.getString(R.string.twoPlayerOnlineScore)
                    .replace("[1]", v.gameModel.numJogadores.value!![0].name)
                    .replace("[A]", v.gameModel.numJogadores.value!![0].score.toString())
                    .replace("[2]", v.gameModel.numJogadores.value!![1].name)
                    .replace("[B]", v.gameModel.numJogadores.value!![1].score.toString())
        }
        else if(v.gameModel.numJogadores.value!!.size == 3) {
            pontuacoesInfo.text = resources.getString(R.string.threePlayerScore)
                    .replace("[1]", v.gameModel.numJogadores.value!![0].name)
                    .replace("[A]", v.gameModel.numJogadores.value!![0].score.toString())
                    .replace("[2]", v.gameModel.numJogadores.value!![1].name)
                    .replace("[B]", v.gameModel.numJogadores.value!![1].score.toString())
                    .replace("[3]", v.gameModel.numJogadores.value!![2].name)
                    .replace("[C]", v.gameModel.numJogadores.value!![2].score.toString())
        }

        if(it.photo != null) {
            playerImageView.visibility = View.VISIBLE
            playerImageView.setImageBitmap(it.photo)
        }
        else {
            playerImageView.visibility = View.GONE
        }
    }

   /* private val observeEndGame = Observer<EndGameStates> {
        when (it) {
            EndGameStates.FINISHED, EndGameStates.ABRUPTLY -> v.calculateWinner()
        }
    }*/

    private val observeWinner = Observer<Jogador> {
        winnerObsTriggered = true
        FirestoreUtils.postFirestoreData(it, v.gameModel.numJogadores.value!!.size, v.gameModel.occupiedPlaces.value!!)


        val jsonData = JSONObject().put(ConstStrings.TYPE, ConstStrings.GAME_END_ABRUPTLY)
            .put(ConstStrings.PLAYER_NAME, it.name).put(ConstStrings.PLAYER_SCORE, it.score)

        for(i in v.gameModel.numJogadores.value!!) {
            if(i.gameSocket != null) {
                NetworkUtils.sendInfo(i.gameSocket!!, jsonData.toString())
            }
        }

        /*when (v.gameModel.endGame.value!!) {
            EndGameStates.FINISHED -> {
                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.endGame))
                        .setMessage(resources.getString(R.string.finalMessage)
                                .replace("[X]", it.id.toString())
                                .replace("[Y]", it.score.toString()))
                        .setCancelable(false)
                        .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                            finish()
                        }
                        .show()

            }

            EndGameStates.ABRUPTLY -> {
                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.endGame))
                        .setMessage(resources.getString(R.string.endGameAbruptly)
                                .replace("[X]", it.id.toString())
                                .replace("[Y]", it.score.toString()))
                        .setCancelable(false)
                        .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                            finish()
                        }
                        .show()
            }
        }*/
    }

    private val observePlayerMoves = Observer<ArrayList<Posicoes>> {
        if(shouldSeeMoves) {
            if(v.gameModel.playerTurn.value!!.id == myPlayerId) {
                adapter.setPlayerMoves(it)
            }
            else {
                adapter.setPlayerMoves(arrayListOf())
            }

            adapter.notifyDataSetChanged()
        }

        //If the current player cannot play, we should save that
        if(it.size == 0) {
            v.gameModel.playerTurn.value!!.hadMoves = false
        }

        //Check if all the players can play
        v.checkPlay()

        if(v.gameModel.playerTurn.value!!.id == myPlayerId) {
            if(v.gameModel.playPositions.value!!.size > 0) {
                passTurnBtn.visibility = View.GONE
            }
            else {
                passTurnBtn.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Function that receives information from server
     */
    private fun receiveGameInfo() {
        thread {
            val json = JSONObject()
            json.put(ConstStrings.TYPE, ConstStrings.CLIENT_WANT_DATA)
            NetworkUtils.sendInfo(LobbyManager.gameSocket!!, json.toString())

            while (gameRunning) {
                var str: String = ""
                try {
                    str = BufferedReader(InputStreamReader(LobbyManager.gameSocket!!.getInputStream())).readLine()
                }
                catch (e: Exception) {
                    return@thread
                }

                //If something makes this var false, it means that the game has ended
                if(!gameRunning) {
                    return@thread
                }

                try {
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

                                if (scores != null && scores.length() == 2) {
                                    val p1 = scores.getJSONObject(0)
                                    val p2 = scores.getJSONObject(1)
                                    pontuacoesInfo.text = resources.getString(R.string.twoPlayerOnlineScore)
                                            .replace("[1]", p1.optString(ConstStrings.PLAYER_NAME))
                                            .replace("[A]", p1.optString(ConstStrings.PLAYER_SCORE))
                                            .replace("[2]", p2.optString(ConstStrings.PLAYER_NAME))
                                            .replace("[B]", p2.optString(ConstStrings.PLAYER_SCORE))
                                }
                                else if(scores != null && scores.length() == 3) {
                                    val p1 = scores.getJSONObject(0)
                                    val p2 = scores.getJSONObject(1)
                                    val p3 = scores.getJSONObject(2)
                                    pontuacoesInfo.text = resources.getString(R.string.threePlayerScore)
                                            .replace("[1]", p1.optString(ConstStrings.PLAYER_NAME))
                                            .replace("[A]", p1.optString(ConstStrings.PLAYER_SCORE))
                                            .replace("[2]", p2.optString(ConstStrings.PLAYER_NAME))
                                            .replace("[B]", p2.optString(ConstStrings.PLAYER_SCORE))
                                            .replace("[3]", p3.optString(ConstStrings.PLAYER_NAME))
                                            .replace("[C]", p3.optString(ConstStrings.PLAYER_SCORE))
                                }

                                playerTurnInfo.text = resources.getString(R.string.player).replace("[X]", currPlayerId.toString())
                                        .plus("\n").plus(resources.getString(R.string.moreInfoPlayer).replace("[Y]", currPlayer.optString(ConstStrings.PLAYER_NAME)))

                                val photoObj = currPlayer.optJSONObject(ConstStrings.PLAYER_PHOTO)
                                if (photoObj != null) {
                                    playerImageView.visibility = View.VISIBLE
                                    playerImageView.setImageBitmap(OtheloUtils.getBitmapFromString(photoObj.optString(ConstStrings.PLAYER_PHOTO)))
                                }
                            }
                        }

                        ConstStrings.GAME_PASS_TURN -> {
                            val player = json.optJSONObject(ConstStrings.CURRENT_PLAYER)
                            currPlayerId = player.optInt(ConstStrings.PLAYER_ID)

                            runOnUiThread {
                                playerTurnInfo.text = resources.getString(R.string.player).replace("[X]", currPlayerId.toString())
                                        .plus("\n").plus(resources.getString(R.string.moreInfoPlayer).replace("[Y]", player.optString(ConstStrings.PLAYER_NAME)))

                                val photoObj = player.optJSONObject(ConstStrings.PLAYER_PHOTO)
                                if (photoObj != null) {
                                    playerImageView.visibility = View.VISIBLE
                                    playerImageView.setImageBitmap(OtheloUtils.getBitmapFromString(photoObj.optString(ConstStrings.PLAYER_PHOTO)))
                                }
                                val json = JSONObject().put(ConstStrings.TYPE, ConstStrings.GAME_UPDATE_INFOS)
                                NetworkUtils.sendInfo(LobbyManager.gameSocket!!, json.toString())
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

                            if(json.optBoolean(ConstStrings.GAME_VALID_PIECE)) {
                                currPlayerId = currPlayer.optInt(ConstStrings.PLAYER_ID)

                                for (i in 0 until newPos.length()) {
                                    adapter.setPositionBoard(newPos.optJSONObject(i).optInt(ConstStrings.BOARD_LINE),
                                            newPos.optJSONObject(i).optInt(ConstStrings.BOARD_COLUMN),
                                            newPos.optJSONObject(i).optInt(ConstStrings.BOARD_POS_VALUE))
                                }

                                if (json.optBoolean(ConstStrings.GAME_VALID_PIECE) && newPos != null) {
                                    runOnUiThread {
                                        if (currPlayerId != myPlayerId) {
                                            adapter.setPlayerMoves(arrayListOf())
                                        }
                                        else {
                                            val moves = json.optJSONArray(ConstStrings.GAME_POSSIBLE_POSITIONS)
                                            val movesArray = arrayListOf<Posicoes>()

                                            if(moves != null) {
                                                for (i in 0 until moves.length()) {
                                                    val movesObj = moves.optJSONObject(i)
                                                    movesArray.add(Posicoes(movesObj.optInt(ConstStrings.BOARD_LINE), movesObj.optInt(ConstStrings.BOARD_COLUMN)))
                                                }
                                            }
                                            adapter.setPlayerMoves(movesArray)
                                        }
                                        adapter.notifyDataSetChanged()

                                        if(currPlayerId == myPlayerId) {
                                            if (json.optInt(ConstStrings.GAME_NUMBER_MOVES) > 0) {
                                                passTurnBtn.visibility = View.GONE
                                            } else {
                                                passTurnBtn.visibility = View.VISIBLE
                                            }
                                        }

                                        if (scores.length() == 2) {
                                            val p1 = scores.getJSONObject(0)
                                            val p2 = scores.getJSONObject(1)
                                            pontuacoesInfo.text = resources.getString(R.string.twoPlayerOnlineScore)
                                                    .replace("[1]", p1.optString(ConstStrings.PLAYER_NAME) ?: p1.optInt(ConstStrings.PLAYER_ID).toString())
                                                    .replace("[A]", p1.optString(ConstStrings.PLAYER_SCORE))
                                                    .replace("[2]", p2.optString(ConstStrings.PLAYER_NAME) ?: p2.optInt(ConstStrings.PLAYER_ID).toString())
                                                    .replace("[B]", p2.optString(ConstStrings.PLAYER_SCORE))
                                        }
                                        else if(scores.length() == 3) {
                                            val p1 = scores.getJSONObject(0)
                                            val p2 = scores.getJSONObject(1)
                                            val p3 = scores.getJSONObject(2)
                                            pontuacoesInfo.text = resources.getString(R.string.threePlayerScore)
                                                    .replace("[1]", p1.optString(ConstStrings.PLAYER_NAME))
                                                    .replace("[A]", p1.optString(ConstStrings.PLAYER_SCORE))
                                                    .replace("[2]", p2.optString(ConstStrings.PLAYER_NAME))
                                                    .replace("[B]", p2.optString(ConstStrings.PLAYER_SCORE))
                                                    .replace("[3]", p3.optString(ConstStrings.PLAYER_NAME))
                                                    .replace("[C]", p3.optString(ConstStrings.PLAYER_SCORE))
                                        }

                                        playerTurnInfo.text = resources.getString(R.string.player).replace("[X]", currPlayerId.toString())
                                                .plus("\n").plus(resources.getString(R.string.moreInfoPlayer).replace("[Y]", currPlayer.optString(ConstStrings.PLAYER_NAME)))

                                        val photoObj = currPlayer.optJSONObject(ConstStrings.PLAYER_PHOTO)
                                        if (photoObj != null) {
                                            playerImageView.visibility = View.VISIBLE
                                            playerImageView.setImageBitmap(OtheloUtils.getBitmapFromString(photoObj.optString(ConstStrings.PLAYER_PHOTO)))
                                        } else {
                                            playerImageView.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }

                        ConstStrings.GAME_POSSIBLE_POSITIONS -> {
                            val moves = json.optJSONArray(ConstStrings.GAME_POSSIBLE_POSITIONS)
                            val movesArray = arrayListOf<Posicoes>()

                            for(i in 0 until moves.length()) {
                                val movesObj = moves.optJSONObject(i)
                                movesArray.add(Posicoes(movesObj.optInt(ConstStrings.BOARD_LINE), movesObj.optInt(ConstStrings.BOARD_COLUMN)))
                            }
                            adapter.setPlayerMoves(movesArray)

                            runOnUiThread {
                                adapter.notifyDataSetChanged()
                            }
                        }

                        ConstStrings.GAME_END_ABRUPTLY -> {
                            gameRunning = false
                            winnerObsTriggered = true
                            val name = json.optString(ConstStrings.PLAYER_NAME)
                            val score = json.optInt(ConstStrings.PLAYER_SCORE)
                            runOnUiThread {
                                AlertDialog.Builder(this)
                                    .setTitle(resources.getString(R.string.endGame))
                                    .setMessage(
                                        resources.getString(R.string.endGameAbruptly)
                                            .replace("[X]", name.toString())
                                            .replace("[Y]", score.toString()))
                                    .setCancelable(false)
                                    .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                                        finish()
                                    }
                                    .show()
                            }
                        }
                    }
                } catch (e : JSONException) {
                    Log.i("GameOnlineActivity", e.printStackTrace().toString())
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.game_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.showMoves -> movesAction()
            R.id.bombTrigger -> bombAction()
            R.id.pieceTrigger -> pieceAction()
            R.id.updateInfos -> {
                val json = JSONObject().put(ConstStrings.TYPE, ConstStrings.GAME_UPDATE_INFOS)
                NetworkUtils.sendInfo(LobbyManager.gameSocket!!, json.toString())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun movesAction() {
        when(connType) {
            ConnType.SERVER -> {
                if(v.gameModel.playerTurn.value!!.id == myPlayerId) {
                    shouldSeeMoves = !shouldSeeMoves
                    if (shouldSeeMoves)
                        adapter.setPlayerMoves(v.gameModel.playPositions.value!!)
                    else
                        adapter.setPlayerMoves(arrayListOf())

                    adapter.notifyDataSetChanged()
                }
            }

            ConnType.CLIENT -> {
                if(currPlayerId == myPlayerId) {
                    val json = JSONObject()
                    json.put(ConstStrings.TYPE, ConstStrings.GAME_PLAYER_SEE_MOVES)
                    NetworkUtils.sendInfo(LobbyManager.gameSocket!!, json.toString())
                }
            }
        }
    }

    private fun bombAction() {
        when(connType) {
            ConnType.SERVER -> {
                if(v.gameModel.playerTurn.value!!.id == myPlayerId) {
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
                if(currPlayerId == myPlayerId) {
                    val json = JSONObject()
                    json.put(ConstStrings.TYPE, ConstStrings.GAME_BOMB_MOVE_ON)
                    NetworkUtils.sendInfo(LobbyManager.gameSocket!!, json.toString())
                }
            }
        }
    }

    private fun pieceAction() {
        when(connType) {
            ConnType.SERVER -> {
                if(v.gameModel.playerTurn.value!!.id == myPlayerId) {
                    if (!v.gameModel.bombMove.value!!) {
                        if (!v.gameModel.changePiecesMove.value!!) {
                            v.gameModel.changePiecesMove.value = true
                            Snackbar.make(gameLayout,
                                    resources.getString(R.string.changePieceSpecial) + " " + resources.getString(R.string.activated),
                                    Snackbar.LENGTH_LONG).show()
                        } else {
                            v.gameModel.changePiecesMove.value = false
                            v.gameModel.changePieceArray.value?.clear()
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
                if(currPlayerId == myPlayerId) {
                    val json = JSONObject()
                    json.put(ConstStrings.TYPE, ConstStrings.GAME_PIECE_MOVE_ON)
                    NetworkUtils.sendInfo(LobbyManager.gameSocket!!, json.toString())
                }
            }
        }
    }

    override fun onDestroy() {
        when(connType) {
            ConnType.SERVER -> {
                if(!winnerObsTriggered) {
                    v.serverLeaveGame()
                    FirestoreUtils.postFirestoreData(v.gameModel.playerWinner.value!!, v.gameModel.numJogadores.value!!.size, v.gameModel.occupiedPlaces.value!!)
                }
                GameRepository.resetGameModel()
                LobbyManager.resetManager()
            }

            ConnType.CLIENT -> {
                gameRunning = false
                if (!winnerObsTriggered) {
                    val json = JSONObject()
                    json.put(ConstStrings.TYPE, ConstStrings.GAME_END_ABRUPTLY)
                    NetworkUtils.sendInfo(LobbyManager.gameSocket!!, json.toString())
                }
            }
        }

        super.onDestroy()
    }

}