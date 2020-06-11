package com.redinput.dualmaps.data

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.redinput.dualmaps.Geocode
import com.redinput.dualmaps.helpers.SingletonHolder

class GeocoderRepository private constructor(context: Context) : GeocoderSource {

    companion object : SingletonHolder<GeocoderRepository, Context>(::GeocoderRepository) {
        private const val MAX_RESULTS = 1
    }

    private var geocoder = Geocoder(context)

    override fun getLocationFromName(name: String): Geocode.Location? {
        val results = geocoder.getFromLocationName(name, MAX_RESULTS)
        val address = results.getOrElse(0) { return null }
        return Geocode.Location(address.latitude, address.longitude)
    }

    override fun getAddressFromLocation(location: LatLng): Geocode.Address? {
        val results = geocoder.getFromLocation(location.latitude, location.longitude, MAX_RESULTS)
        val address = results.getOrElse(0) { return null }

        val parts = mutableListOf<String>()
        address.thoroughfare?.let { parts.add(it) }
        address.subThoroughfare?.let { parts.add(it) }
        val title = parts.joinToString(", ")

        parts.clear()
        address.postalCode?.let { parts.add(it) }
        address.locality?.let { parts.add(it) }
        address.countryName?.let { parts.add(it) }
        val subtitle = parts.joinToString(", ")

        return Geocode.Address(title, subtitle)
    }
}