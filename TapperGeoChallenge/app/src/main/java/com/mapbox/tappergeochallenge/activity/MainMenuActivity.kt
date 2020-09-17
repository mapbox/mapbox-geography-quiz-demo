package com.mapbox.tappergeochallenge.activity

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.tappergeochallenge.R
import com.mapbox.tappergeochallenge.StringConstants
import kotlinx.android.synthetic.main.activity_main_menu.single_player_game_button
import kotlinx.android.synthetic.main.activity_main_menu.two_player_game_button

/**
 * This class handles the selection of a one or two player game,
 * entering the two players' names, and any other pre-game
 * initialization that might be needed.
 */
class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        single_player_game_button.setOnClickListener {
            val intent = Intent(applicationContext, GameActivity::class.java)
            intent.putExtra(StringConstants.TYPE_OF_GAME_KEY, StringConstants.ONE_PLAYER_GAME)
            startActivity(intent)
        }
        two_player_game_button.setOnClickListener {
            showPlayerNameDialog()
        }
    }

    private fun showPlayerNameDialog() {
        val dialogView = this.layoutInflater.inflate(R.layout.player_name_dialog, null)
        AlertDialog.Builder(this).apply {
            setView(dialogView)
            val playerOneName = dialogView.findViewById<EditText>(R.id.player_one_editText_name)
            val playerTwoName = dialogView.findViewById<EditText>(R.id.player_two_editText_name)
            setTitle(R.string.dialog_title)
            setPositiveButton(getString(R.string.dialog_positive_button)) { dialog, whichButton ->
                Intent(applicationContext, GameActivity::class.java).apply {
                    putExtra(StringConstants.TYPE_OF_GAME_KEY, StringConstants.TWO_PLAYER_GAME)
                    putExtra(StringConstants.PLAYER_ONE_NAME_KEY, playerOneName.text.toString())
                    putExtra(StringConstants.PLAYER_TWO_NAME_KEY, playerTwoName.text.toString())
                    startActivity(this)
                }
            }
            create().show()
        }
    }
}