package com.redinput.dualmaps.data

import com.redinput.dualmaps.Geocode

interface GeocoderSource {
    fun getLocationFromName(name: String): Geocode.Response?
}