package com.mapbox.tappergeochallenge.model

import com.mapbox.mapboxsdk.geometry.LatLng

/**
 * This class represents a city that will be guessed in the game.
 */
data class City(
        var name: String?,
        var latLng: LatLng? = null
)