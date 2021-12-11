package com.example.otello.network.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.otello.R
import com.example.otello.network.ConnType
import com.example.otello.network.LobbyStates
import com.example.otello.network.viewmodel.NetworkVM
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.OtheloUtils
import kotlinx.android.synthetic.main.activity_network.*
import java.io.ByteArrayOutputStream


class NetworkActivity : AppCompatActivity() {

    var networkVM : NetworkVM? = null
    var connType : ConnType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)

        networkVM = ViewModelProvider(this).get(NetworkVM::class.java)
        connType = ConnType.valueOf(intent.getStringExtra(ConstStrings.INTENT_CONN_TYPE).toString())

        val pref = getSharedPreferences(ConstStrings.SHARED_PREFERENCES_INSTANCE, Context.MODE_PRIVATE)
        val playerName = pref.getString(ConstStrings.SHARED_PREFERENCES_NAME, "").toString()
        val playerPhotoPath = pref.getString(ConstStrings.SHARED_PREFERENCES_PHOTO, "").toString()

        if(connType == ConnType.SERVER) {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            val ipAddress = String.format("%d.%d.%d.%d", ip and 0xff, (ip shr 8) and 0xff, (ip shr 16) and 0xff, (ip shr 24) and 0xff)

            //Create the bitmap to add to player object
            val imageBitmap = BitmapFactory.decodeFile(playerPhotoPath)
            val rotation = OtheloUtils.rotateBitmap(playerPhotoPath)
            val realBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.width, imageBitmap.height, rotation, true)

            networkVM!!.initServer(playerName, realBitmap)
            ipTextView.text = ipAddress
            networkVM!!.clientsConnected.observe(this, observeNumClients)
        }
        else {
            val connIp = intent.getStringExtra(ConstStrings.INTENT_IP_ADDR)

            //Conversão da imagem para a enviar pelo socket
            val bm = BitmapFactory.decodeFile(playerPhotoPath)
            val rotation = OtheloUtils.rotateBitmap(playerPhotoPath)
            val realBitmap = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, rotation, true)

            val baos = ByteArrayOutputStream()
            realBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos) // bm is the bitmap object
            val encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.URL_SAFE)

            networkVM!!.initClient(connIp!!, playerName, encodedImage)
            ipTextView.text = connIp
            networkVM!!.infos.observe(this, obsInsfos)
        }
    }

    private val observeNumClients = Observer<Int> {
        connClients.text = "Clients: ${it}"
    }

    private val obsInsfos = Observer<LobbyStates> {
        when(it) {
            LobbyStates.GAME_STARTING -> infos.text = "Game is starting"
            LobbyStates.GAME_STOPPED -> {
                Toast.makeText(this, "Server Stopped the game", Toast.LENGTH_SHORT).show()
                finish()
            }
            LobbyStates.WAITING_START -> infos.text = "Waiting for server to start game"
            LobbyStates.SENDING_INFO -> infos.text = "Sending player name and photo to server..."
            LobbyStates.TOO_MANY_PLAYERS -> {
                Toast.makeText(this, "You cannot join game, lobby is full", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(connType == ConnType.SERVER) {
            networkVM!!.killServer()
        }
        else {
            networkVM!!.clientLeave()
        }
    }


}