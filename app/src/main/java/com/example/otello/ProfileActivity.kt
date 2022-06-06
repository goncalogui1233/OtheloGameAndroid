package com.example.otello

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.otello.photo.PhotoDialog2
import com.example.otello.photo.PhotoPreview
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.OtheloUtils
import kotlinx.android.synthetic.main.activity_profile.*
import java.io.File

class ProfileActivity : AppCompatActivity() {

    var playerName : String = ""
    var photoName : String = ""
    var menu : Menu? = null
    var editMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        readFromSharedPreferences()

        nome_jogador.setText(playerName)

        setPlayerImage()

        btnCamera.setOnClickListener {
            PhotoDialog2().apply {
                saveListener = PhotoPreview.OnSavePhoto {
                    photoName = getSharedPreferences(ConstStrings.SHARED_PREFERENCES_INSTANCE, Context.MODE_PRIVATE)
                        .getString(ConstStrings.SHARED_PREFERENCES_PHOTO, "").toString()

                    setPlayerImage()
                }
                show(supportFragmentManager, "PhotoDialog")

            }
        }
    }

    private fun setPlayerImage() {
        val imgPath = File(photoName)
        if(imgPath.exists()){
            val b = BitmapFactory.decodeFile(imgPath.absolutePath)
            val rotation = OtheloUtils.rotateBitmap(imgPath.absolutePath)

            val bit = Bitmap.createBitmap(b, 0,0, b.width, b.height, rotation, true)

            player_image.setImageBitmap(bit)
        }
    }

    private fun readFromSharedPreferences() {
        getSharedPreferences(ConstStrings.SHARED_PREFERENCES_INSTANCE, Context.MODE_PRIVATE).apply {
            playerName = getString(ConstStrings.SHARED_PREFERENCES_NAME, "").toString()
            photoName = getString(ConstStrings.SHARED_PREFERENCES_PHOTO, "").toString()
        }
    }

    private fun saveOnSharedPreferences() {
        getSharedPreferences(ConstStrings.SHARED_PREFERENCES_INSTANCE, Context.MODE_PRIVATE).edit {
            putString(ConstStrings.SHARED_PREFERENCES_NAME, playerName)
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