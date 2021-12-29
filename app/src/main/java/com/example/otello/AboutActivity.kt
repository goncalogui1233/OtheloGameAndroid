package com.example.otello

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val creator1 = "Davide Coelho nº 2017013544"
        val creator2 = "Gonçalo Guilherme nº 2016020759"

        davideTxtView.text = creator1
        davideImgView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.foto_davide))

        goncaloTxtView.text = creator2
        goncaloImgView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.foto_goncalo))
    }
}