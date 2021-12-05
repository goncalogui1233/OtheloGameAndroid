package com.example.otello.network.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.otello.network.manager.NetworkManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

class NetworkVM : ViewModel(){

    private var serverSocket : ServerSocket? = null
    private var port : Int = 6021
    var clientsConnected = MutableLiveData(1)
    val infos = MutableLiveData<String>()

    private var stopSocket : Boolean = false
    private var checkClientInfos = false
    private var checkServerInfos = false



    //Server Functions

    fun initServer(){
       thread {
            serverSocket = ServerSocket()
            serverSocket!!.reuseAddress = true
            serverSocket!!.bind(InetSocketAddress(port))
            while(!stopSocket) {
                serverSocket.apply {
                    try {
                        val socket = this?.accept()
                        if (socket != null) {
                            NetworkManager.socketList.add(socket)
                            clientsConnected.postValue(clientsConnected.value!! + 1)

                        }
                    }catch (e : SocketException){ }
                }
            }
        }
    }

    //Function that looks for infos sent to server by clients
    fun informationToServer(socket: Socket) {
        thread {
            while(!checkServerInfos) {
                var str = ""
                try {
                    str = BufferedReader(InputStreamReader(socket.getInputStream())).readLine()
                } catch (_ :Exception) {
                    return@thread
                }

                val json = JSONObject(str)

                when(json.optString("Type")){

                    "Leave_Game" -> {

                    }

                }

            }
        }

    }

    fun killServer(){
        stopSocket = true
        serverSocket?.close()
    }


    //Client Functions

    fun initClient(ip : String){
        thread {
            try {
                NetworkManager.socketEnt = Socket(ip, port)
            }
            catch (_ : Exception) {
                return@thread
            }

            informationToClient()
        }
    }

    //Receive info from server
    fun informationToClient() {
        thread {
            while (!checkClientInfos) {
                var str = ""
                try {
                    str = NetworkManager.receiveInfo(NetworkManager.socketEnt!!)
                } catch (_ :Exception) {
                    return@thread
                }

                val json = JSONObject(str)

                when(json.optString("type")){}
            }
        }
    }




}