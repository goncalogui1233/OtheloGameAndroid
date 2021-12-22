package com.example.otello.game.model

import android.graphics.Bitmap
import java.io.Serializable
import java.net.Socket

class Jogador : Serializable{

    val id : Int
    var name : String = ""
    var bombPiece : Boolean
    var pieceChange : Boolean
    var hadMoves : Boolean
    var seeMoves : Boolean
    var score : Int = 0
    var photo : Bitmap? = null
    var socket : Socket? = null
    var gameSocket : Socket? = null

    constructor(id : Int) {
        this.id = id
        this.bombPiece = true
        this.pieceChange = true
        this.hadMoves = true
        this.seeMoves = false
    }

}