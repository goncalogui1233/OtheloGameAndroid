package com.example.otello.game.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.otello.R

class GameOnline : GameBaseFragment() {

    override fun playerNumber(): Int = 3

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_game, container, false)

        return view
    }


}