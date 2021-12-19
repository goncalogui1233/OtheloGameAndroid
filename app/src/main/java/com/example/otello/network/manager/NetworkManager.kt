package com.example.otello.network.manager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.otello.game.model.GameModel
import com.example.otello.game.model.Jogador
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.OtheloUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.Socket
import kotlin.concurrent.thread

object NetworkManager {

    var socketEnt : Socket? = null

    fun sendInfo(socket: Socket, info : String) {
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