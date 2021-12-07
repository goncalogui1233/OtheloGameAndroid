package com.example.otello

import android.content.Context
import android.content.DialogInterface
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.otello.photo.PhotoDialog2
import kotlinx.android.synthetic.main.activity_profile.*
import java.io.File

class ProfileActivity : AppCompatActivity(), DialogInterface.OnDismissListener {

    var playerName : String = ""
    var photoName : String = ""
    var menu : Menu? = null
    var editMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        readFromSharedPreferences()

        nome_jogador.setText(playerName)

        val imgPath = File(applicationInfo.dataDir + "/pic.jpg")
        if(imgPath.exists()){
            val b = BitmapFactory.decodeFile(imgPath.absolutePath)
            player_image.setImageBitmap(b)
            player_image.rotation = 90f
        }

        btnCamera.setOnClickListener {
            val fr = PhotoDialog2()
            fr.show(supportFragmentManager, "JJJ")
        }
    }

    override fun onDismiss(dialogInt: DialogInterface?) {

        val imgPath = File(applicationInfo.dataDir + "/pic.jpg")

        if(imgPath.exists()){
            val b = BitmapFactory.decodeFile(imgPath.absolutePath)
            player_image.setImageBitmap(b)
            player_image.rotation = 90f
        }
    }

    private fun readFromSharedPreferences() {
        val pref = getSharedPreferences("ProfileInfo", Context.MODE_PRIVATE)

        playerName = pref.getString("PLAYER_NAME", "").toString()
        photoName = pref.getString("PHOTO_NAME", "").toString()
    }

    private fun saveOnSharedPreferences() {
        getSharedPreferences("ProfileInfo", Context.MODE_PRIVATE).edit {
            putString("PLAYER_NAME", playerName)
            putString("PHOTO_NAME", photoName)
            commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.profileEdit){
            if(editMode){
                editMode = false
                deactivateEdit()
                menu?.getItem(0)?.icon = ContextCompat.getDrawable(this, R.drawable.ic_edit)
            }
            else {
                editMode = true
                activateEdit()
                menu?.getItem(0)?.icon = ContextCompat.getDrawable(this, R.drawable.ic_save)
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun activateEdit() {
        nome_jogador.isEnabled = true
        btnCamera.visibility = View.VISIBLE
    }

    private fun deactivateEdit() {
        nome_jogador.isEnabled = false

        btnCamera.visibility = View.GONE
        playerName = nome_jogador.text.toString()
        saveOnSharedPreferences()
    }
}