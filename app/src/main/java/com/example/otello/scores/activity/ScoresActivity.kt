package com.example.otello.scores.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.otello.R
import com.example.otello.scores.adapter.ScoresAdapter
import com.example.otello.scores.model.ScoresClass
import com.example.otello.utils.ConstStrings
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_scores.*


class ScoresActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scores)

        val arr = arrayListOf<ScoresClass>()

        val db = Firebase.firestore

        val collection = db.collection(ConstStrings.FIRESTORE_COLLECTION)

        collection.get()
                .addOnSuccessListener { it ->
            for (i in 0 until it.size()) {
                val name = it.documents[i].get(ConstStrings.FIRESTORE_PLAYER_NAME) as String
                val score = (it.documents[i].get(ConstStrings.FIRESTORE_PLAYER_SCORE) as String).toInt()
                val opponents = (it.documents[i].get(ConstStrings.FIRESTORE_NUMBER_OPPONENTS) as String).toInt()
                val pieces = (it.documents[i].get(ConstStrings.FIRESTORE_PIECES_PLACED) as String).toInt()

                arr.add(ScoresClass(name, score, opponents, pieces))
            }

            arr.sortByDescending { it2 ->
                it2.playerScore
            }

            scoresRecyclerView.layoutManager = LinearLayoutManager(this)
            val adapter = ScoresAdapter(this, arr)
            scoresRecyclerView.adapter = adapter

            scoresProgressBar.visibility = View.GONE
        }
                .addOnFailureListener {
                    Snackbar.make(scoresRootView, "Error while retrieving scores", Snackbar.LENGTH_SHORT).show()
                }


    }
}