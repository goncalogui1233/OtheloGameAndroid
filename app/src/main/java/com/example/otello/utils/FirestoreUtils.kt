package com.example.otello.utils

import android.util.Log
import com.example.otello.game.model.Jogador
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FirestoreUtils {

    fun postFirestoreData(player: Jogador, opponents : Int, filledPieces : Int) {
        val db = Firebase.firestore

        val record = HashMap<String, String>()
        val collection = db.collection("scoreCollection")

        record["Name"] = player.name
        record["Score"] = player.score.toString()
        record["Opponents"] = opponents.toString()
        record["FilledPieces"] = filledPieces.toString()

        collection.get().addOnSuccessListener {
            if (it.size() < 5) { //adiciona logo o jogador
                collection.document((it.size() + 1).toString()).set(record)
            } else { //Dos que existem, verifica o que tem menor pontuação...
                var lowerScore = it.documents[0]
                for (i in 1 until it.size()) {
                    if ((it.documents[i].get("Score") as String).toInt() < (lowerScore.get("Score") as String).toInt()) {
                        lowerScore = it.documents[i]
                    }
                }

                collection.document(lowerScore.id).set(record)
            }
        }
            .addOnFailureListener {
                Log.e("GameOnlineActivity", "Not possible to get data from Firestore")
            }
    }

}