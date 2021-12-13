package com.example.otello.game.model

import androidx.lifecycle.MutableLiveData

object GameModel {

    val board = MutableLiveData<Array<IntArray>>()
    val playerTurn = MutableLiveData<Jogador>()
    val numJogadores = MutableLiveData<ArrayList<Jogador>>(arrayListOf())
    val boardDimensions = MutableLiveData<Int>()
    val playPositions = MutableLiveData<ArrayList<Posicoes>>()

    val endGame = MutableLiveData(false)
    var bombMove = MutableLiveData(false)
    var changePiecesMove = MutableLiveData(false)

    val changePieceArray = arrayListOf<Posicoes>()

}