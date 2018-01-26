package com.mapbox.tappergeochallenge.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.mapbox.tappergeochallenge.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.mapbox.tappergeochallenge.StringConstants.ONE_PLAYER_GAME;
import static com.mapbox.tappergeochallenge.StringConstants.PLAYER_ONE_NAME;
import static com.mapbox.tappergeochallenge.StringConstants.PLAYER_TWO_NAME;
import static com.mapbox.tappergeochallenge.StringConstants.TWO_PLAYER_GAME;
import static com.mapbox.tappergeochallenge.StringConstants.TYPE_OF_GAME;

public class MainMenuActivity extends AppCompatActivity {

  private String TAG = "MainActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main_menu);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.single_player_game_button)
  public void singlePlayerGame(View view) {
    Log.d(TAG, "singlePlayerGame: ");
    Intent intent = new Intent(getApplicationContext(), GameActivity.class);
    intent.putExtra(TYPE_OF_GAME, ONE_PLAYER_GAME);
    startActivity(intent);
  }

  @OnClick(R.id.double_player_game_button)
  public void twoPlayerGame(View view) {
    askAndSetPlayerNames();
  }

  private void askAndSetPlayerNames() {
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
    LayoutInflater inflater = this.getLayoutInflater();
    final View dialogView = inflater.inflate(R.layout.player_name_dialog, null);
    dialogBuilder.setView(dialogView);
    final EditText playerOneName = dialogView.findViewById(R.id.player_one_editText_name);
    final EditText playerTwoName = dialogView.findViewById(R.id.player_two_editText_name);
    dialogBuilder.setTitle(R.string.dialog_title);
    dialogBuilder.setPositiveButton(getString(R.string.dialog_positive_button), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        Log.d(TAG, "twoPlayerGame: ");
        Intent intent = new Intent(getApplicationContext(), GameActivity.class);
        intent.putExtra(TYPE_OF_GAME, TWO_PLAYER_GAME);
        intent.putExtra(PLAYER_ONE_NAME, playerOneName.getText().toString());
        intent.putExtra(PLAYER_TWO_NAME, playerTwoName.getText().toString());
        startActivity(intent);
      }
    });
    AlertDialog nameDialog = dialogBuilder.create();
    nameDialog.show();
  }
}
