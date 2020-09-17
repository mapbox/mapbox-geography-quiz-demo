package com.mapbox.tappergeochallenge.model

import com.mapbox.mapboxsdk.geometry.LatLng

/**
 * This class represents a player that will play the game.
 */
data class Player(var name: String? = "") {
    var selectedLatLng: LatLng? = null
    var points: Double = 0.0
    var hasGuessed: Boolean = false
}