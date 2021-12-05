package com.example.otello.network.activities

import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.otello.R
import com.example.otello.network.ConnType
import com.example.otello.network.viewmodel.NetworkVM
import kotlinx.android.synthetic.main.activity_network.*


class NetworkActivity : AppCompatActivity() {

    var networkVM : NetworkVM? = null
    var ipAddr : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)

        networkVM = ViewModelProvider(this).get(NetworkVM::class.java)

        if(intent.getStringExtra("type") == ConnType.SERVER.toString()) {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            val ipAddress = String.format("%d.%d.%d.%d", ip and 0xff, (ip shr 8) and 0xff, (ip shr 16) and 0xff, (ip shr 24) and 0xff)

            networkVM!!.initServer()
            ipTextView.text = ipAddress
            networkVM!!.clientsConnected.observe(this, observeNumClients)
        }
        else {
            val connIp = intent.getStringExtra("IP")

            networkVM!!.initClient(connIp!!)
            ipTextView.text = connIp
            networkVM!!.infos.observe(this, obsInsfos)
        }
    }

    private val observeNumClients = Observer<Int> {

        connClients.text = "Clients: ${it}"

    }

    private val obsInsfos = Observer<String> {

        infos.text = it

    }

    override fun onDestroy() {
        super.onDestroy()
        networkVM!!.killServer()
    }


}