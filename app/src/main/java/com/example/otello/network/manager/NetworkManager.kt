package com.example.otello.network.manager

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.Socket

object NetworkManager {

    val socketList = arrayListOf<Socket>()
    var socketEnt : Socket? = null

    fun sendInfo(socket: Socket, info : String) {
        val printStream = PrintStream(socket.getOutputStream())

        printStream.println(info)
        printStream.flush()
    }

    fun receiveInfo(socket: Socket) : String {
        return BufferedReader(InputStreamReader(socket.getInputStream())).readLine()
    }

}