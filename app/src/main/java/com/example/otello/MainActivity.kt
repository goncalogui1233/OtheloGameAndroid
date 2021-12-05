package com.example.otello

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.otello.game.activities.GameActivity
import com.example.otello.network.ConnType
import com.example.otello.network.activities.NetworkActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startLocalGame.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        startOnlineGame.setOnClickListener {
            alertDialogOnline()
        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    fun alertDialogOnline(){
        AlertDialog.Builder(this)
            .setTitle("Online Mode")
            .setMessage("Client or Server")
            .setPositiveButton("Server") { _, _ ->
                val it = Intent(this, NetworkActivity::class.java)
                it.putExtra("type", ConnType.SERVER.toString())
                startActivity(it)
            }
            .setNegativeButton("Client") { _, _ ->
                insertIpDialog()
            }
            .show()
    }

    fun insertIpDialog() {
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

        AlertDialog.Builder(this)
            .setTitle("IP Address")
            .setMessage("Insert Server IP Address")
            .setView(edtBox)
            .setPositiveButton("OK") { _, _ ->
                val ip = edtBox.text
                if (ip.isEmpty() || !Patterns.IP_ADDRESS.matcher(ip).matches()) {
                    Toast.makeText(this, "Error in IP inserted", Toast.LENGTH_LONG).show()
                } else {
                    val it = Intent(this, NetworkActivity::class.java)
                    it.putExtra("IP", ip.toString())
                    it.putExtra("type", ConnType.CLIENT.toString())
                    startActivity(it)
                }
            }
            .show()
    }

}