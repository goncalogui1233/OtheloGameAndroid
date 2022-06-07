package com.example.otello.game.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.example.otello.R
import com.example.otello.game.fragments.GameBaseFragment
import com.example.otello.game.fragments.GameOffline
import com.example.otello.game.fragments.GameOnline
import com.example.otello.game.repository.GameRepository
import com.example.otello.network.model.ConnType
import com.example.otello.utils.ConstStrings
import kotlinx.android.synthetic.main.activity_game.*

class GameActivity : AppCompatActivity() {
    lateinit var fragment : GameBaseFragment

    private lateinit var connType : ConnType
    private var myPlayerId : Int = -1
    var gameMode : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        if(intent.hasExtra(ConstStrings.INTENT_CONN_TYPE)) {
            connType = ConnType.valueOf(intent.getStringExtra(ConstStrings.INTENT_CONN_TYPE)!!)
        }

        intent.getStringExtra(ConstStrings.INTENT_GAME_MODE)?.let {
            gameMode = it
        }

        myPlayerId = intent.getIntExtra(ConstStrings.PLAYER_ID, -1)

        when(intent.getStringExtra(ConstStrings.INTENT_GAME_MODE)) {
            ConstStrings.INTENT_GAME_LOCAL -> fragment = GameOffline()
            ConstStrings.INTENT_GAME_ONLINE -> fragment = GameOnline()
        }

        supportFragmentManager.beginTransaction().add(gameContainer.id, fragment).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.game_menu, menu)
        menu?.findItem(R.id.updateInfos)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fragment.menuClicked(item.itemId)
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        GameRepository.resetGameModel()
    }

}