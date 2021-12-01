package com.example.otello.game.model

class Jogador {

    val id : Int
    var bombPiece : Boolean
    var pieceChange : Boolean
    var playedLastTime : Boolean
    var score : Int = 0

    constructor(id : Int) {
        this.id = id
        this.bombPiece = true
        this.pieceChange = true
        this.playedLastTime = true
    }

}