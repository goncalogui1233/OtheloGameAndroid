package com.example.otello.game.viewmodel

import androidx.lifecycle.ViewModel
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class NetworkVM : ViewModel(){

    private var serverSocket : ServerSocket? = null
    private val socketList = arrayListOf<Socket>()
    private var port : Int = 6021
    private var clientsConnected : Int = 1


    fun initServer(){
        thread {
            serverSocket = ServerSocket(port)
            while(true) {
                serverSocket.apply {
                    val socket = serverSocket!!.accept()
                    socketList.add(socket)
                    clientsConnected++
                }
            }
        }
    }
}