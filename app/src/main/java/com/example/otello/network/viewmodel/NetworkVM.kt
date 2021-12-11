package com.example.otello.network.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.util.Base64
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.otello.game.model.Jogador
import com.example.otello.network.LobbyStates
import com.example.otello.network.manager.NetworkManager
import com.example.otello.utils.ConstStrings
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*
import kotlin.concurrent.thread


class NetworkVM : ViewModel(){

    private var serverSocket : ServerSocket? = null
    private var port : Int = 6021
    var clientsConnected = MutableLiveData(1)
    val infos = MutableLiveData<LobbyStates>()
    val jogadores = arrayListOf<Jogador>()

    private var stopSocket : Boolean = false
    private var checkClientInfos = false
    private var checkServerInfos = false

    //Server Functions

    fun initServer(nome: String, image: Bitmap){
        //Adicionar o próprio jogador à lista
        val pl = Jogador(clientsConnected.value!!)
        pl.name = nome
        pl.photo = image
        jogadores.add(pl)

       thread {
            serverSocket = ServerSocket()
            serverSocket!!.reuseAddress = true
            serverSocket!!.bind(InetSocketAddress(port))
            while(!stopSocket) {
                serverSocket.apply {
                    try {
                        val socket = this?.accept()
                        if (socket != null) {
                            val p = Jogador(clientsConnected.value!! + 1)
                            p.socket = socket
                            clientsConnected.postValue(clientsConnected.value!! + 1)
                            informationToServer(p)
                            jogadores.add(p)
                        }
                    } catch (e: SocketException) { }
                }
            }
        }
    }

    //Function that looks for infos sent to server by clients
    fun informationToServer(player : Jogador) {
        thread {
            while(!checkServerInfos) {
                var str = ""
                try {
                    str = BufferedReader(InputStreamReader(player.socket?.getInputStream())).readLine()
                } catch (_: Exception) {
                    Log.i("InfoToServer", "Error in read")
                    jogadores.remove(player)
                    clientsConnected.postValue(clientsConnected.value!! - 1)
                    return@thread
                }

                val json = JSONObject(str)

                when(json.getString(ConstStrings.TYPE)){
                    ConstStrings.PLAYER_INFO -> {
                        player.name = json.optString(ConstStrings.PLAYER_INFO_NOME)
                        val bytePhoto = Base64.decode(json.optString(ConstStrings.PLAYER_INFO_PHOTO), Base64.URL_SAFE)
                        player.photo = BitmapFactory.decodeByteArray(bytePhoto, 0, bytePhoto.size)

                        player.photo = Bitmap.createBitmap(player.photo!!, 0,0, player.photo!!.width, player.photo!!.height, null, true)

                        val jsonObj = JSONObject()
                        jsonObj.put(ConstStrings.TYPE, ConstStrings.PLAYER_INFO_RESPONSE)
                        jsonObj.put(ConstStrings.PLAYER_INFO_RESPONSE_VALID, true)
                        NetworkManager.sendInfo(player.socket!!, jsonObj.toString())
                    }

                    ConstStrings.LEAVE_GAME -> {
                        clientsConnected.postValue(clientsConnected.value!! - 1)
                        jogadores.remove(player)
                        return@thread
                    }
                }
            }
        }

    }

    //Server cancels game
    fun killServer(){
        thread {
            stopSocket = true
            serverSocket?.close()
            val json = JSONObject()
            json.put(ConstStrings.TYPE, ConstStrings.STOP_GAME)
            for(p in jogadores) {
                if(p.socket != null) {
                    NetworkManager.sendInfo(p.socket!!, json.toString())
                }
            }
        }
    }


    //Client Functions

    //Init Socket Client, send his data to server and open thread to listen to events from server
    fun initClient(ip: String, nome: String, encodedImage: String){
        thread {
            try {
                NetworkManager.socketEnt = Socket(ip, port)
            }
            catch (_: Exception) {
                return@thread
            }

            //Envia nome e foto ao servidor
            infos.postValue(LobbyStates.SENDING_INFO)

            val json = JSONObject()
            json.put(ConstStrings.TYPE, ConstStrings.PLAYER_INFO)
            json.put(ConstStrings.PLAYER_INFO_NOME, nome)
            json.put(ConstStrings.PLAYER_INFO_PHOTO, encodedImage)

            NetworkManager.sendInfo(NetworkManager.socketEnt!!, json.toString())

            informationToClient()
        }
    }

    //Client receive info from server
    fun informationToClient() {
        thread {
            while (!checkClientInfos) {
                var str = ""
                try {
                    str = NetworkManager.receiveInfo(NetworkManager.socketEnt!!)
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
                        if(json.optBoolean(ConstStrings.PLAYER_INFO_RESPONSE_VALID)) {
                            infos.postValue(LobbyStates.WAITING_START)
                        }
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
            if(NetworkManager.socketEnt != null) {
                NetworkManager.sendInfo(NetworkManager.socketEnt!!, json.toString())
                NetworkManager.socketEnt = null
            }
        }
    }




}