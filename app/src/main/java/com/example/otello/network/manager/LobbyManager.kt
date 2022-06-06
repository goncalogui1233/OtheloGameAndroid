package com.example.otello.network.manager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.otello.game.model.Jogador
import com.example.otello.game.repository.GameRepository
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.NetworkUtils
import org.json.JSONObject
import java.net.Socket
import kotlin.concurrent.thread

object LobbyManager {

    var socketEnt : Socket? = null
    var gameSocket : Socket? = null

    private val playerId : MutableLiveData<Int> = MutableLiveData(-1)
    private var clientsConnected = MutableLiveData(0)
    private val jogadores = arrayListOf<Jogador>()

    fun addNewPlayer(isServer : Boolean, name : String = "", photo : Bitmap? = null, socketLobby : Socket? = null, socketGame : Socket? = null) {
        val p = Jogador(clientsConnected.value!! + 1)

        if(isServer) {
            p.name = name.ifEmpty { ConstStrings.PREDEFINED_PLAYER_NAME }
            p.photo = photo
            playerId.postValue(p.id)
        }
        else {
            p.lobbySocket = socketLobby
            p.gameSocket = socketGame
            clientsConnected.postValue(clientsConnected.value!! + 1)
        }

        jogadores.add(p)
    }

    fun fillPlayerInfo(json: JSONObject, player: Jogador): Boolean {
        if (clientsConnected.value!! <= 3) {
            val nome = json.optString(ConstStrings.PLAYER_INFO_NOME)
            player.name = nome.ifEmpty { ConstStrings.PREDEFINED_PLAYER_NAME }

            val bytePhoto = Base64.decode(json.optString(ConstStrings.PLAYER_INFO_PHOTO), Base64.URL_SAFE)
            val rawBitmap = BitmapFactory.decodeByteArray(bytePhoto, 0, bytePhoto.size)
            if (rawBitmap != null) {
                player.photo = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, null, true)
            }

            val jsonObj = JSONObject()
            jsonObj.put(ConstStrings.TYPE, ConstStrings.PLAYER_INFO_RESPONSE)
            jsonObj.put(ConstStrings.PLAYER_INFO_RESPONSE_VALID, ConstStrings.PLAYER_INFO_RESPONSE_ACCEPTED)
            jsonObj.put(ConstStrings.PLAYER_ID, player.id)
            NetworkUtils.sendInfo(player.lobbySocket!!, jsonObj.toString())
            return true
        } else {
            val jsonObj = JSONObject()
            jsonObj.put(ConstStrings.TYPE, ConstStrings.PLAYER_INFO_RESPONSE)
            jsonObj.put(ConstStrings.PLAYER_INFO_RESPONSE_VALID, ConstStrings.PLAYER_INFO_TOO_MANY_PLAYERS)
            NetworkUtils.sendInfo(player.lobbySocket!!, jsonObj.toString())
            clientsConnected.postValue(clientsConnected.value!! - 1)
            jogadores.remove(player)
            return false
        }
    }

    fun removePlayer(player: Jogador) {
        clientsConnected.postValue(clientsConnected.value!! - 1)

        val pos = jogadores.indexOf(player) + 1
        for(i in pos until jogadores.size){
            jogadores[i].id = jogadores[i].id - 1
            val json = JSONObject().put(ConstStrings.TYPE, ConstStrings.UPDATE_ID)
                .put(ConstStrings.NEW_ID, jogadores[i].id)
            NetworkUtils.sendInfo(jogadores[i].lobbySocket!!, json.toString())
        }

        jogadores.remove(player)
    }

    fun removePlayerError(player : Jogador) {
        jogadores.remove(player)
        clientsConnected.postValue(clientsConnected.value!! - 1)
    }

    /**
     * Method that returns the last added player
     */
    fun getLastAddedPlayer() : Jogador {
        return jogadores.last()
    }

    /**
     * Method that informs all players that the game is going to start
     */
    fun warnGameStart() {
        GameRepository.numJogadores.value = jogadores

        val json = JSONObject()
        json.put(ConstStrings.TYPE, ConstStrings.START_GAME)

        for(p in jogadores) {
            if(p.lobbySocket != null) {
                NetworkUtils.sendInfo(p.lobbySocket!!, json.toString())
            }
        }
    }

    /**
     * Method that informs all players that the game is cancelled
     */
    fun warnGameCancelled() {
        thread {
            val json = JSONObject()
            json.put(ConstStrings.TYPE, ConstStrings.STOP_GAME)
            for(p in jogadores) {
                if(p.lobbySocket != null) {
                    NetworkUtils.sendInfo(p.lobbySocket!!, json.toString())
                }
            }
        }
    }

    fun getConnectedPlayers() : LiveData<Int> {
        return clientsConnected
    }

    fun getPlayerId() : MutableLiveData<Int> {
        return playerId
    }



}