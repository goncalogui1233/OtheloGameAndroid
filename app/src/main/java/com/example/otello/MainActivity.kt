package com.example.otello

import android.content.Intent
import android.net.InetAddresses
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.otello.game.activities.GameActivity
import com.example.otello.network.model.ConnType
import com.example.otello.network.activities.LobbyActivity
import com.example.otello.scores.activity.ScoresActivity
import com.example.otello.utils.ConstStrings
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startLocalGame.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java).apply {
                putExtra(ConstStrings.INTENT_GAME_MODE, ConstStrings.INTENT_GAME_LOCAL)
            })
        }

        startOnlineGame.setOnClickListener {
            alertDialogOnline()
        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        scoresBtn.setOnClickListener {
            startActivity(Intent(this, ScoresActivity::class.java))
        }

        aboutBtn.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

    }

    private fun alertDialogOnline(){
        AlertDialog.Builder(this).apply {
            title = resources.getString(R.string.onlineMode)
            setMessage(resources.getString(R.string.clientServer))
            setPositiveButton(resources.getString(R.string.server)) { _, _ ->
                val it = Intent(this@MainActivity, LobbyActivity::class.java).apply {
                    putExtra(ConstStrings.INTENT_CONN_TYPE, ConnType.SERVER.toString())
                }
                startActivity(it)
            }
            setNegativeButton(resources.getString(R.string.client)) { _, _ ->
                insertIpDialog()
            }
            show()
        }
    }

    private fun insertIpDialog() {
        val edtBox = EditText(this).apply {
            maxLines = 1
            filters = arrayOf(object : InputFilter {
                override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
                    if (source?.none { it.isDigit() || it == '.' } == true)
                        return ""
                    return null
                }
            })
        }

        AlertDialog.Builder(this).apply {
            title = resources.getString(R.string.ipAddress)
            setMessage(resources.getString(R.string.insertIP))
            setView(edtBox)
            setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                val ip = edtBox.text
                if (ip.isEmpty() || !checkIpAddress(ip.toString())) {
                    Toast.makeText(this@MainActivity, resources.getString(R.string.errorIP), Toast.LENGTH_LONG).show()
                } else {
                    val it = Intent(this@MainActivity, LobbyActivity::class.java).apply {
                        putExtra(ConstStrings.INTENT_IP_ADDR, ip.toString())
                        putExtra(ConstStrings.INTENT_CONN_TYPE, ConnType.CLIENT.toString())
                    }
                    startActivity(it)
                }
            }
            show()
        }
    }

    private fun checkIpAddress(ip : String) : Boolean {

        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            InetAddresses.isNumericAddress(ip)
        } else {
            Patterns.IP_ADDRESS.matcher(ip).matches()
        }

    }

}