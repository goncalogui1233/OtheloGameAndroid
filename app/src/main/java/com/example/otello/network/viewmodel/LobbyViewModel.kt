package com.example.otello.network.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.otello.game.model.Jogador
import com.example.otello.network.model.LobbyStates
import com.example.otello.network.manager.LobbyManager
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.NetworkUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import kotlin.concurrent.thread


class LobbyViewModel : ViewModel(){

    private var serverSocket : ServerSocket? = null
    private var port : Int = 6021

    var stopSocket : Boolean = false
    var checkClientInfos = false
    var checkServerInfos = false

    val infos = MutableLiveData<LobbyStates>()

    //Server Functions
    fun initServer(nome: String, image: Bitmap?){
        //Adicionar o próprio jogador à lista
        LobbyManager.addNewPlayer(true, nome, image)

        thread {
            serverSocket = ServerSocket()
            serverSocket!!.reuseAddress = true
            serverSocket!!.bind(InetSocketAddress(port))
            while(!stopSocket) {
                serverSocket.apply {
                    try {
                        val socket = this?.accept()
                        if (socket != null) {
                            val socket2 = this?.accept()
                            LobbyManager.addNewPlayer(isServer = false, socketLobby = socket, socketGame = socket2);
                            informationClientToServer(LobbyManager.getLastAddedPlayer())
                        }
                    } catch (e: SocketException) { }
                }
            }
        }
    }

    //Function that looks for infos sent to server by clients
    fun informationClientToServer(player : Jogador) {
        thread {
            while(!checkServerInfos) {
                var str = ""
                try {
                    str = BufferedReader(InputStreamReader(player.lobbySocket?.getInputStream())).readLine()
                }
                catch (e: Exception) {
                    Log.i("InfoToServer", "Error reading player info")
                    LobbyManager.removePlayerError(player)
                    return@thread
                }

                try {
                    val json = JSONObject(str)

                    when (json.getString(ConstStrings.TYPE)) {
                        ConstStrings.PLAYER_INFO -> {
                            LobbyManager.fillPlayerInfo(json, player)
                        }

                        ConstStrings.LEAVE_GAME -> {
                            LobbyManager.removePlayer(player)
                            return@thread
                        }

                        ConstStrings.PLAYER_ENTER_GAME -> {
                            return@thread
                        }
                    }
                } catch (e : JSONException) { }
            }
        }
    }

    //Server warns every player that the game will start
    fun startGame() {
        LobbyManager.warnGameStart()
    }

    fun stopServerSocket() {
        stopSocket = true
        serverSocket?.close()
    }

    //Server cancels game
    fun killServer(){
        stopSocket = true
        serverSocket?.close()
        LobbyManager.warnGameCancelled()
    }


    //Client Functions

    //Init Socket Client, send his data to server and open thread to listen to events from server
    fun initClient(ip: String, nome: String, encodedImage: String){
        thread {
            try {
                LobbyManager.socketEnt = Socket(ip, port)
                LobbyManager.gameSocket = Socket(ip, port)
            }
            catch (e: Exception) {
                return@thread
            }

            //Envia nome e foto ao servidor
            infos.postValue(LobbyStates.SENDING_INFO)

            val json = JSONObject()
            json.put(ConstStrings.TYPE, ConstStrings.PLAYER_INFO)
            json.put(ConstStrings.PLAYER_INFO_NOME, nome)
            json.put(ConstStrings.PLAYER_INFO_PHOTO, encodedImage)

            NetworkUtils.sendInfo(LobbyManager.socketEnt!!, json.toString())

            informationServerToClient()
        }
    }

    //Client receive info from server
    fun informationServerToClient() {
       thread {
            while (!checkClientInfos) {
                var str = ""
                try {
                    str = NetworkUtils.receiveInfo(LobbyManager.socketEnt!!)
                } catch (_: Exception) {
                    return@thread
                }

                val json = JSONObject(str)

                when(json.optString(ConstStrings.TYPE)){
                    ConstStrings.START_GAME -> {
                        infos.postValue(LobbyStates.GAME_STARTING)
                        return@thread
                    }
                    ConstStrings.STOP_GAME -> {
                        infos.postValue(LobbyStates.GAME_STOPPED)
                        return@thread
                    }
                    ConstStrings.PLAYER_INFO_RESPONSE -> {
                        if(json.optString(ConstStrings.PLAYER_INFO_RESPONSE_VALID) == ConstStrings.PLAYER_INFO_RESPONSE_ACCEPTED) {
                            LobbyManager.getPlayerId().postValue(json.optInt(ConstStrings.PLAYER_ID))
                            infos.postValue(LobbyStates.WAITING_START)
                        }
                        else {
                            infos.postValue(LobbyStates.TOO_MANY_PLAYERS)
                        }
                    }
                    ConstStrings.UPDATE_ID -> {
                        LobbyManager.getPlayerId().postValue(json.optInt(ConstStrings.NEW_ID))
                    }
                }
            }
        }
    }

    //Client sends request to leave game
    fun clientLeave() {
        thread {
            val json = JSONObject()
            json.put(ConstStrings.TYPE, ConstStrings.LEAVE_GAME)
            if(LobbyManager.socketEnt != null) {
                NetworkUtils.sendInfo(LobbyManager.socketEnt!!, json.toString())
                LobbyManager.socketEnt = null
            }
        }
    }

    fun getConnectedClients() : LiveData<Int> {
        return LobbyManager.getConnectedPlayers()
    }

    fun getPlayerId() : LiveData<Int> {
        return LobbyManager.getPlayerId()
    }

}