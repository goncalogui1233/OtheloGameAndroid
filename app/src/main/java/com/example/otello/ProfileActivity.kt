package com.example.otello

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.otello.photo.PhotoActivity
import kotlinx.android.synthetic.main.activity_profile.*
import java.io.File

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        btnCamera.setOnClickListener {
            startActivity(Intent(this, PhotoActivity::class.java))
        }

        val imgPath = File(applicationInfo.dataDir + "/pic.jpg")

        if(imgPath.exists()){
            val b = BitmapFactory.decodeFile(imgPath.absolutePath)
            player_image.setImageBitmap(b)
            player_image.rotation = 90f
        }
    }
}