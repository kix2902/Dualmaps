package com.redinput.dualmaps.data

import android.content.Context
import android.location.Geocoder
import com.redinput.dualmaps.Geocode
import com.redinput.dualmaps.helpers.SingletonHolder

class GeocoderRepository private constructor(context: Context) : GeocoderSource {

    companion object : SingletonHolder<GeocoderRepository, Context>(::GeocoderRepository) {
        private const val MAX_RESULTS = 1
    }

    private var geocoder = Geocoder(context)

    override fun getLocationFromName(name: String): Geocode.Response? {
        val results = geocoder.getFromLocationName(name, MAX_RESULTS)
        val address = results.getOrElse(0) { return null }
        return Geocode.Response(address.latitude, address.longitude)
    }
}