package com.example.otello.network.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.otello.R
import com.example.otello.game.activities.GameOnlineActivity
import com.example.otello.game.repository.GameRepository
import com.example.otello.network.model.ConnType
import com.example.otello.network.model.LobbyStates
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

        //Get profile data from Shared Preferences
        val pref = getSharedPreferences(ConstStrings.SHARED_PREFERENCES_INSTANCE, Context.MODE_PRIVATE)
        val playerName = pref.getString(ConstStrings.SHARED_PREFERENCES_NAME, "").toString()
        val playerPhotoPath = pref.getString(ConstStrings.SHARED_PREFERENCES_PHOTO, "").toString()

        if(connType == ConnType.SERVER) {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            val ipAddress = String.format("%d.%d.%d.%d", ip and 0xff, (ip shr 8) and 0xff, (ip shr 16) and 0xff, (ip shr 24) and 0xff)

            //Create the bitmap to add to player object
            val imageBitmap = BitmapFactory.decodeFile(playerPhotoPath)
            var realBitmap : Bitmap? = null
            if(imageBitmap != null) {
                val rotation = OtheloUtils.rotateBitmap(playerPhotoPath)
                realBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.width, imageBitmap.height, rotation, true)
            }
            networkVM!!.initServer(playerName, realBitmap)
            ipTextView.text = ipAddress
            networkVM!!.clientsConnected.observe(this, observeNumClients)

            btnStartGame.setOnClickListener {
                btnStartGame.isEnabled = false
                GameRepository.numJogadores.value = networkVM!!.jogadores
                networkVM!!.checkServerInfos = true
                networkVM!!.stopServerSocket()
                networkVM!!.startGame()

                val intent = Intent(this, GameOnlineActivity::class.java)
                intent.putExtra(ConstStrings.INTENT_CONN_TYPE, connType.toString())
                intent.putExtra(ConstStrings.INTENT_GAME_MODE, ConstStrings.INTENT_GAME_ONLINE)

                startActivity(intent)
                finish()
            }
        }
        else {
            val connIp = intent.getStringExtra(ConstStrings.INTENT_IP_ADDR)
            var encodedImage : String = ""

            //Conversão da imagem para a enviar pelo socket
            if(playerPhotoPath != "") {
                val bm = BitmapFactory.decodeFile(playerPhotoPath)
                val rotation = OtheloUtils.rotateBitmap(playerPhotoPath)
                val realBitmap = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, rotation, true)

                val baos = ByteArrayOutputStream()
                realBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos) // bm is the bitmap object
                encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.URL_SAFE)
            }

            networkVM!!.initClient(connIp!!, playerName, encodedImage)
            ipTextView.text = connIp
            networkVM!!.infos.observe(this, obsInsfos)
        }
    }

    private val observeNumClients = Observer<Int> {
        connClients.text = resources.getString(R.string.playerNumber).replace("[X]", it.toString())

        if(it > 1) {
            btnStartGame.visibility = View.VISIBLE
        }
        else {
            btnStartGame.visibility = View.GONE
        }

    }

    private val obsInsfos = Observer<LobbyStates> {
        when(it) {
            LobbyStates.GAME_STARTING -> {
                infos.text = resources.getString(R.string.gameStarting)
                networkVM!!.checkClientInfos = true
                //networkVM!!.clientEnterGame()

                //TODO -> Remove this sleep...
                Thread.sleep(2000)

                val intent = Intent(this, GameOnlineActivity::class.java)
                intent.putExtra(ConstStrings.INTENT_CONN_TYPE, connType.toString())
                intent.putExtra(ConstStrings.INTENT_GAME_MODE, ConstStrings.INTENT_GAME_ONLINE)
                startActivity(intent)
                finish()
            }
            LobbyStates.GAME_STOPPED -> {
                Toast.makeText(this, resources.getString(R.string.serverStopGame), Toast.LENGTH_SHORT).show()
                finish()
            }
            LobbyStates.WAITING_START -> infos.text = resources.getString(R.string.waitingStart)
            LobbyStates.SENDING_INFO -> infos.text = resources.getString(R.string.sendingInfo)
            LobbyStates.TOO_MANY_PLAYERS -> {
                Toast.makeText(this, resources.getString(R.string.lobbyFull), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (connType == ConnType.SERVER) {
            networkVM!!.killServer()
        } else {
            networkVM!!.clientLeave()
        }
    }

}