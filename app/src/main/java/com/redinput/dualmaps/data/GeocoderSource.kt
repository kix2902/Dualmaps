package com.redinput.dualmaps.data

import com.google.android.gms.maps.model.LatLng
import com.redinput.dualmaps.Geocode

interface GeocoderSource {
    fun getLocationFromName(name: String): Geocode.Location?
    fun getAddressFromLocation(location: LatLng): Geocode.Address?
}