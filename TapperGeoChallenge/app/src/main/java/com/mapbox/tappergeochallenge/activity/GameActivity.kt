package com.mapbox.tappergeochallenge.activity

import android.graphics.BitmapFactory
import android.icu.text.NumberFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.tappergeochallenge.R
import com.mapbox.tappergeochallenge.StringConstants
import com.mapbox.tappergeochallenge.model.City
import com.mapbox.tappergeochallenge.model.Player
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_game_layout.check_answer_fab
import kotlinx.android.synthetic.main.activity_game_layout.location_to_guess_textview
import kotlinx.android.synthetic.main.activity_game_layout.mapView
import kotlinx.android.synthetic.main.activity_game_layout.player_one_points_textview
import kotlinx.android.synthetic.main.activity_game_layout.player_two_points_textview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.util.Locale.getDefault
import java.util.Random

/**
 * This activity handles the main guessing game play on top of a Mapbox Maps SDK map.
 */
open class GameActivity : AppCompatActivity(), OnMapReadyCallback, MapboxMap.OnMapClickListener {
    private var mapboxMap: MapboxMap? = null
    private var currentCityToGuess: City? = null
    private var isSinglePlayerGame = false
    private var playerOneSymbol: Symbol? = null
    private var playerTwoSymbol: Symbol? = null
    private var bullsEyeSymbol: Symbol? = null
    private lateinit var symbolManager: SymbolManager
    private lateinit var playerOne: Player
    private var playerTwo: Player? = null
    private lateinit var listOfCityFeatures: List<Feature>
    private var textViewFlashingAnimation: Animation = AlphaAnimation(0.0f, 1.0f).apply {
        duration = 100 // Manage the blinking speed here
        startOffset = 1
        repeatMode = Animation.REVERSE
        repeatCount = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the MapView.
        // TODO: Make sure that you add your Mapbox token into this project's strings.xml file!
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        // This contains the mapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_game_layout)

        // Determine whether a one or two person game was selected in the previous activity/screen
        isSinglePlayerGame = intent?.getStringExtra(StringConstants.TYPE_OF_GAME_KEY) == StringConstants.ONE_PLAYER_GAME

        // Set the visibility of parts of the card view that's on top of the Mapbox map
        playerOne = Player(intent?.getStringExtra(StringConstants.PLAYER_ONE_NAME_KEY))

        if (!isSinglePlayerGame) {
            player_one_points_textview.visibility = View.VISIBLE
            player_two_points_textview.visibility = View.VISIBLE
            playerTwo = Player(intent.getStringExtra(StringConstants.PLAYER_TWO_NAME_KEY)!!)
            displayPlayersPoints()
        }
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this@GameActivity.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.Builder().fromUri(Style.MAPBOX_STREETS)
                .withImage(PLAYER_ONE_ICON_ID, BitmapFactory.decodeResource(this.resources, R.drawable.player_one_icon))
                .withImage(PLAYER_TWO_ICON_ID, BitmapFactory.decodeResource(this.resources, R.drawable.player_two_icon))
                .withImage(BULLSEYE_ICON_ID, BitmapFactory.decodeResource(this.resources, R.drawable.target_bullseye_icon))
        ) {
            CoroutineScope(context = Dispatchers.Default + Job()).launch {
                try {
                    // Use fromJson() method to convert GeoJSON file into a usable FeatureCollection
                    val featureCollection = FeatureCollection.fromJson(loadGeoJsonFromAsset("cities.geojson"))
                    listOfCityFeatures = featureCollection.features()!!

                    runOnUiThread {
                        // Retrieve the first city to guess
                        setNewRandomCityToGuess()

                        // Set up a SymbolManager instance
                        symbolManager = SymbolManager(mapView, mapboxMap, it)
                        symbolManager.iconAllowOverlap = true
                        symbolManager.iconIgnorePlacement = true

                        mapboxMap.addOnMapClickListener(this@GameActivity)

                        initAnswerButton()
                    }
                } catch (exception: Exception) {
                    Log.d(TAG, "getFeatureCollectionFromJson exception: $exception")
                }
            }
        }
    }

    /**
     * Set the onClickListener for the [check_answer_fab].
     */
    private fun initAnswerButton() {
        check_answer_fab.setOnClickListener {
            if (isSinglePlayerGame) {
                Snackbar.make(findViewById(android.R.id.content),
                        resources.getString(R.string.player_guess_distance,
                                getDistanceBetweenTargetAndGuess(playerOne)),
                        Snackbar.LENGTH_SHORT).show()
                resetUiAfterAnswerDistanceCheck()
                check_answer_fab.hide()
            } else {
                if (!playerOne.hasGuessed) {
                    playerOne.hasGuessed = true
                    Toast.makeText(this, String.format(getString(R.string.player_two_turn_to_guess), playerTwo!!.name), Toast.LENGTH_SHORT).show()
                    check_answer_fab.hide()
                } else if (playerOne.hasGuessed && !playerTwo?.hasGuessed!!) {
                    calculateAndGivePointToWinner()
                    resetUiAfterAnswerDistanceCheck()
                    playerOne.hasGuessed = false
                    playerTwo?.hasGuessed = false
                    check_answer_fab.hide()
                }
            }
        }
    }

    /**
     * Reset UI once a city guess round has finished.
     */
    private fun resetUiAfterAnswerDistanceCheck() {
        setBullsEyeMarker(currentCityToGuess?.latLng!!)
        setCameraBoundsToSelectedAndTargetMarkers()
        setNewRandomCityToGuess()
    }

    /**
     * Process the onMapClick logic based on whether it's a multi-player
     * game and who already guessed.
     */
    override fun onMapClick(mapClickPoint: LatLng): Boolean {
        if (isSinglePlayerGame) {
            setPlayerOneMarker(mapClickPoint)
            playerOne.selectedLatLng = mapClickPoint
        } else {
            if (!playerOne.hasGuessed && !playerTwo!!.hasGuessed) {
                setPlayerOneMarker(mapClickPoint)
                playerOne.selectedLatLng = mapClickPoint
            } else if (playerOne.hasGuessed && !playerTwo!!.hasGuessed) {
                setPlayerTwoMarker(mapClickPoint)
                playerTwo!!.selectedLatLng = mapClickPoint
            }
        }
        check_answer_fab.show()
        return true
    }

    /**
     * Create/adjust the first player's guess icon.
     */
    private fun setPlayerOneMarker(newLatLng: LatLng) {
        if (playerOneSymbol == null) {
            playerOneSymbol = symbolManager.create(SymbolOptions()
                    .withLatLng(newLatLng)
                    .withIconImage(PLAYER_ONE_ICON_ID)
                    .withIconSize(ICON_SIZE)
                    .withDraggable(false))
        } else {
            playerOneSymbol!!.let {
                it.latLng = newLatLng
                symbolManager.update(it)
            }
        }
    }

    /**
     * Create/adjust the second player's guess icon.
     */
    private fun setPlayerTwoMarker(newLatLng: LatLng) {
        if (playerTwoSymbol == null) {
            playerTwoSymbol = symbolManager.create(SymbolOptions()
                    .withLatLng(newLatLng)
                    .withIconImage(PLAYER_TWO_ICON_ID)
                    .withIconSize(ICON_SIZE)
                    .withDraggable(false))
        } else {
            playerTwoSymbol!!.let {
                it.latLng = newLatLng
                symbolManager.update(it)
            }
        }
    }

    /**
     * Create/adjust the target city icon.
     */
    private fun setBullsEyeMarker(bullsEyeLocation: LatLng) {
        if (bullsEyeSymbol == null) {
            bullsEyeSymbol = symbolManager.create(SymbolOptions()
                    .withLatLng(bullsEyeLocation)
                    .withIconImage(BULLSEYE_ICON_ID)
                    .withIconSize(ICON_SIZE)
                    .withDraggable(false))
        } else {
            bullsEyeSymbol!!.let {
                it.latLng = bullsEyeLocation
                symbolManager.update(it)
            }
        }
    }

    /**
     * Move the map camera to show certain coordinates.
     */
    private fun setCameraBoundsToSelectedAndTargetMarkers() {
        val latLngBounds: LatLngBounds = if (isSinglePlayerGame) {
            LatLngBounds.Builder()
                    .include(playerOne.selectedLatLng!!)
                    .include(currentCityToGuess!!.latLng!!)
                    .build()
        } else {
            LatLngBounds.Builder()
                    .include(currentCityToGuess!!.latLng!!)
                    .include(playerOne.selectedLatLng!!)
                    .include(playerTwo!!.selectedLatLng!!)
                    .build()
        }
        mapboxMap?.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,
                CAMERA_BOUNDS_PADDING), EASE_CAMERA_SPEED_IN_MS)
    }

    /**
     * Retrieve and set a new city as the target city to guess.
     */
    private fun setNewRandomCityToGuess() {
        if (listOfCityFeatures.isNotEmpty()) {
            val randomCityFromList = listOfCityFeatures[Random().nextInt(listOfCityFeatures.size).plus(1)]
            val randomCityAsPoint = randomCityFromList.geometry() as Point
            currentCityToGuess = City(randomCityFromList.getStringProperty(FEATURE_CITY_PROPERTY_KEY),
                    LatLng(randomCityAsPoint.latitude(), randomCityAsPoint.longitude())).also {
                location_to_guess_textview.text = resources.getString(R.string.location_to_guess,
                        it.name)
            }
        }
    }

    /**
     * Uses Mapbox's Turf library to get the as-the-crow-flies distance between two
     * [Point]s.
     */
    private fun getDistanceBetweenTargetAndGuess(playerToCheck: Player?): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NumberFormat.getNumberInstance(getDefault()).format(TurfMeasurement.distance(
                    Point.fromLngLat(currentCityToGuess!!.latLng?.longitude!!,
                            currentCityToGuess!!.latLng?.latitude!!),
                    Point.fromLngLat(playerToCheck!!.selectedLatLng?.longitude!!,
                            playerToCheck.selectedLatLng?.latitude!!)))
        } else {
            TurfMeasurement.distance(
                    Point.fromLngLat(currentCityToGuess!!.latLng?.longitude!!,
                            currentCityToGuess!!.latLng?.latitude!!),
                    Point.fromLngLat(playerToCheck!!.selectedLatLng?.longitude!!,
                            playerToCheck.selectedLatLng?.latitude!!)).toString()
        }
    }

    /**
     * If it's a multi-player game, calculate which player's guess was closer to the
     * target city
     */
    private fun calculateAndGivePointToWinner() {
        when (getDistanceBetweenTargetAndGuess(playerOne) < getDistanceBetweenTargetAndGuess(playerTwo)) {
            true -> {
                playerOne.points = playerOne.points.plus(1)
                player_one_points_textview?.startAnimation(textViewFlashingAnimation)
            }
            false -> {
                playerTwo?.points = playerTwo?.points!!.plus(1)
                player_two_points_textview.startAnimation(textViewFlashingAnimation)
            }
        }
        Snackbar.make(findViewById(android.R.id.content),
                resources.getString(R.string.winner_announcement,
                        if (getDistanceBetweenTargetAndGuess(playerOne) <
                                getDistanceBetweenTargetAndGuess(playerTwo))
                            playerOne.name else playerTwo!!.name),
                Snackbar.LENGTH_SHORT).show()
        displayPlayersPoints()
    }

    /**
     * Update the players' point [TextView]s.
     */
    private fun displayPlayersPoints() {
        setPlayerTextViews(player_one_points_textview, R.string.player_one_points, if (playerOne.name!!.isEmpty()) getString(R.string.default_player_one_name) else playerOne.name!!, playerOne.points.toInt())
        setPlayerTextViews(player_two_points_textview, R.string.player_two_points, if (playerTwo?.name!!.isEmpty()) getString(R.string.default_player_two_name) else playerTwo!!.name!!, playerTwo!!.points.toInt())
    }

    private fun setPlayerTextViews(textView: TextView, stringId: Int, playerName: String, numOfPoints: Int) {
        textView.text = String.format(getString(stringId), playerName, numOfPoints, getString(R.string.points))
    }

    /**
     * Load the file that has the list of cities to guess from.
     */
    private fun loadGeoJsonFromAsset(filename: String): String {
        return try {
            // Load GeoJSON file from local asset folder
            val `is` = assets.open(filename)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (exception: Exception) {
            throw RuntimeException(exception)
        }
    }

    // region activity lifecycle method overrides
    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxMap?.removeOnMapClickListener(this)
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    } //endregion

    companion object {
        private const val CAMERA_BOUNDS_PADDING = 190
        private const val EASE_CAMERA_SPEED_IN_MS = 1000
        private const val FEATURE_CITY_PROPERTY_KEY = "city"
        private const val ICON_SIZE = 1.2f
        private const val PLAYER_ONE_ICON_ID = "PLAYER_ONE_ICON_ID"
        private const val PLAYER_TWO_ICON_ID = "PLAYER_TWO_ICON_ID"
        private const val BULLSEYE_ICON_ID = "BULLSEYE_ICON_ID"
        private const val TAG = "GameActivity"
    }
}