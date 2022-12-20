package com.example.otello.game.model

sealed class GameStates {
    object Abruptly : GameStates()
    object Playing : GameStates()
    class EndGame(val winner : Jogador? = null) : GameStates()
}
