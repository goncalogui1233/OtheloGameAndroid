package com.example.otello.scores.model

class ScoresClass {

    val playerName : String
    val playerScore : Int
    val opponentNumber : Int
    val piecesPlaced : Int


    constructor(playerName : String, playerScore : Int, opponentNumber : Int, piecesPlaced : Int) {
        this.playerName = playerName
        this.playerScore = playerScore
        this.opponentNumber = opponentNumber
        this.piecesPlaced = piecesPlaced
    }

}