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
import com.example.otello.game.model.EndGameStates
import com.example.otello.game.model.GameModel
import com.example.otello.game.model.Jogador
import com.example.otello.game.model.Posicoes
import com.example.otello.game.viewmodel.GameOnlineViewModel
import com.example.otello.network.manager.NetworkManager
import com.example.otello.network.model.ConnType
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.OtheloUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_game.*
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
                    if(v.gameModel.playerTurn.value?.id == NetworkManager.playerId) {
                        v.gameModel.playerTurn.value = v.checkNextPlayer()
                        val jsonData = JSONObject()
                        jsonData.put(ConstStrings.TYPE, ConstStrings.GAME_PASS_TURN)
                        val nextPlayer = JSONObject().put(ConstStrings.PLAYER_ID, v.gameModel.playerTurn.value!!.id)
                                .put(ConstStrings.PLAYER_NAME, v.gameModel.playerTurn.value!!.name)
                        if (v.gameModel.playerTurn.value!!.photo != null) {
                            nextPlayer.put(ConstStrings.PLAYER_PHOTO, OtheloUtils.getStringFromBitmap(v.gameModel.playerTurn.value!!.photo!!))
                        }

                        jsonData.put(ConstStrings.CURRENT_PLAYER, nextPlayer)

                        for(i in v.gameModel.numJogadores.value!!) {
                            if(i.gameSocket != null) {
                                NetworkManager.sendInfo(i.gameSocket!!, jsonData.toString())
                            }
                        }
                    }
                }
                ConnType.CLIENT -> {
                    if(currPlayerId == NetworkManager.playerId) {
                        val json = JSONObject()
                        json.put(ConstStrings.TYPE, ConstStrings.GAME_PASS_TURN)
                        NetworkManager.sendInfo(NetworkManager.gameSocket!!, json.toString())
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

                    NetworkManager.sendInfo(NetworkManager.gameSocket!!, json.toString())
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
        playerTurnInfo.text = resources.getString(R.string.player)
                .replace("[X]", v.gameModel.playerTurn.value?.id.toString())

        //Ao mudar o jogador, atualizar as pontuações
        pontuacoesInfo.text = resources.getString(R.string.twoPlayerScore)
                .replace("[A]", v.gameModel.numJogadores.value!![0].score.toString())
                .replace("[B]", v.gameModel.numJogadores.value!![1].score.toString())

        if(it.photo != null) {
            playerImageView.visibility = View.VISIBLE
            playerImageView.setImageBitmap(it.photo)
        }
        else {
            playerImageView.visibility = View.GONE
        }
    }

    private val observeEndGame = Observer<EndGameStates> {
        when (it) {
            EndGameStates.FINISHED, EndGameStates.ABRUPTLY -> v.calculateWinner()
        }
    }

    private val observeWinner = Observer<Jogador> {
        winnerObsTriggered = true
        postFirestoreData(it)

        val jsonData = JSONObject().put(ConstStrings.TYPE, ConstStrings.GAME_END_ABRUPTLY)
            .put(ConstStrings.PLAYER_NAME, it.name).put(ConstStrings.PLAYER_SCORE, it.score)

        for(i in v.gameModel.numJogadores.value!!) {
            if(i.gameSocket != null) {
                NetworkManager.sendInfo(i.gameSocket!!, jsonData.toString())
            }
        }

        when (v.gameModel.endGame.value!!) {
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
        }
    }

    private val observePlayerMoves = Observer<ArrayList<Posicoes>> {
        if(shouldSeeMoves) {
            adapter.setPlayerMoves(it)
            adapter.notifyDataSetChanged()
        }

        if(v.gameModel.playerTurn.value!!.id == NetworkManager.playerId) {
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
            NetworkManager.sendInfo(NetworkManager.gameSocket!!, json.toString())

            while (gameRunning) {
                var str: String = ""
                try {
                    str = BufferedReader(InputStreamReader(NetworkManager.gameSocket!!.getInputStream())).readLine()
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
                                playerTurnInfo.text = "Jogador: " + currPlayerId.toString() + "\nNome: " + player.optString(ConstStrings.PLAYER_NAME)
                                val photoObj = player.optJSONObject(ConstStrings.PLAYER_PHOTO)
                                if (photoObj != null) {
                                    playerImageView.visibility = View.VISIBLE
                                    playerImageView.setImageBitmap(OtheloUtils.getBitmapFromString(photoObj.optString(ConstStrings.PLAYER_PHOTO)))
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

                            if(json.optBoolean(ConstStrings.GAME_VALID_PIECE)) {
                                currPlayerId = currPlayer.optInt(ConstStrings.PLAYER_ID)

                                for (i in 0 until newPos.length()) {
                                    adapter.setPositionBoard(newPos.optJSONObject(i).optInt(ConstStrings.BOARD_LINE),
                                            newPos.optJSONObject(i).optInt(ConstStrings.BOARD_COLUMN),
                                            newPos.optJSONObject(i).optInt(ConstStrings.BOARD_POS_VALUE))
                                }

                                if (json.optBoolean(ConstStrings.GAME_VALID_PIECE) && newPos != null) {
                                    runOnUiThread {
                                        if (currPlayerId != NetworkManager.playerId) {
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

                                        if(currPlayerId == NetworkManager.playerId) {
                                            if (json.optInt(ConstStrings.GAME_NUMBER_MOVES) > 0) {
                                                passTurnBtn.visibility = View.GONE
                                            } else {
                                                passTurnBtn.visibility = View.VISIBLE
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

                                        playerTurnInfo.text = "Jogador: " + currPlayer.optInt(ConstStrings.PLAYER_ID).toString() + "\nNome: " + currPlayer.optString(ConstStrings.PLAYER_NAME)
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
                NetworkManager.sendInfo(NetworkManager.gameSocket!!, json.toString())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun movesAction() {
        when(connType) {
            ConnType.SERVER -> {
                if(v.gameModel.playerTurn.value!!.id == NetworkManager.playerId) {
                    shouldSeeMoves = !shouldSeeMoves
                    if (shouldSeeMoves)
                        adapter.setPlayerMoves(v.gameModel.playPositions.value!!)
                    else
                        adapter.setPlayerMoves(arrayListOf())

                    adapter.notifyDataSetChanged()
                }
            }

            ConnType.CLIENT -> {
                if(currPlayerId == NetworkManager.playerId) {
                    val json = JSONObject()
                    json.put(ConstStrings.TYPE, ConstStrings.GAME_PLAYER_SEE_MOVES)
                    NetworkManager.sendInfo(NetworkManager.gameSocket!!, json.toString())
                }
            }
        }
    }

    private fun bombAction() {
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
                    NetworkManager.sendInfo(NetworkManager.gameSocket!!, json.toString())
                }
            }
        }
    }

    private fun pieceAction() {
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
                    NetworkManager.sendInfo(NetworkManager.gameSocket!!, json.toString())
                }
            }
        }
    }

    fun postFirestoreData(player: Jogador) {
        val db = Firebase.firestore

        val record = HashMap<String, String>()
        val collection = db.collection("scoreCollection")

        record["Name"] = player.name
        record["Score"] = player.score.toString()
        record["Opponents"] = v.gameModel.numJogadores.value!!.size.toString()
        record["FilledPieces"] = v.gameModel.occupiedPlaces.value!!.toString()

        collection.get().addOnSuccessListener {
                    if (it.size() < 5) { //adiciona logo o jogador
                        collection.document((it.size() + 1).toString()).set(record)
                    } else { //Dos que existem, verifica o que tem menor pontuação...
                        var lowerScore = it.documents[0]
                        for (i in 1 until it.size()) {
                            if ((it.documents[i].get("Score") as String).toInt() < (lowerScore.get("Score") as String).toInt()) {
                                lowerScore = it.documents[i]
                            }
                        }

                        collection.document(lowerScore.id).set(record)
                    }
                }
                .addOnFailureListener {
                    Log.e("GameOnlineActivity", "Not possible to get data from Firestore")
                }
    }

    override fun onDestroy() {
        when(connType) {
            ConnType.SERVER -> {
                if(!winnerObsTriggered) {
                    v.serverLeaveGame()
                    postFirestoreData(v.gameModel.playerWinner.value!!)
                }
                GameModel.resetGameModel()
            }

            ConnType.CLIENT -> {
                gameRunning = false
                val json = JSONObject()
                json.put(ConstStrings.TYPE, ConstStrings.GAME_END_ABRUPTLY)
                NetworkManager.sendInfo(NetworkManager.gameSocket!!, json.toString())
            }
        }

        super.onDestroy()
    }

}