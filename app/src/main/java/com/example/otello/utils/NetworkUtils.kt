package com.example.otello.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.Socket
import kotlin.concurrent.thread

object NetworkUtils {

    fun sendInfo(socket: Socket, info : String){
        thread {
            val printStream = PrintStream(socket.getOutputStream())

            printStream.println(info)
            printStream.flush()
        }
    }

    fun receiveInfo(socket: Socket) : String {
        return BufferedReader(InputStreamReader(socket.getInputStream())).readLine()
    }

}