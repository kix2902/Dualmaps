package com.redinput.dualmaps.ui.main

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.hadilq.liveevent.LiveEvent
import com.redinput.dualmaps.Geocode
import com.redinput.dualmaps.LocationStatus
import com.redinput.dualmaps.R
import com.redinput.dualmaps.TAG
import com.redinput.dualmaps.data.GeocoderRepository
import com.redinput.dualmaps.data.NetworkRepository
import com.redinput.dualmaps.domain.GetLocationFromQuery
import com.redinput.dualmaps.domain.GetRandomCoordinates
import com.redinput.dualmaps.domain.UseCase.None
import com.redinput.dualmaps.domain.UseCase.Result

@SuppressLint("MissingPermission")
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val MIN_ACCURACY = 100
    }

    private val liveStatus = MutableLiveData<LocationStatus?>(null)
    private val liveLoading = MutableLiveData(false)
    private val liveMessage = LiveEvent<Int>()

    private val fusedProvider =
        LocationServices.getFusedLocationProviderClient(application.applicationContext)
    private val locationRequest = LocationRequest.create()?.apply {
        interval = 1000
        fastestInterval = 500
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val getRandomCoordinates = GetRandomCoordinates(viewModelScope, NetworkRepository)
    private val getLocationFromQuery = GetLocationFromQuery(
        viewModelScope,
        GeocoderRepository.getInstance(application.applicationContext)
    )

    fun getObservableStatus(): LiveData<LocationStatus?> = liveStatus
    fun getObservableLoading(): LiveData<Boolean> = liveLoading
    fun getObservableMessage(): LiveData<Int> = liveMessage

    fun getRandomLocation() {
        liveLoading.value = true
        getRandomCoordinates.invoke(None()) {
            liveLoading.value = false
            when (it) {
                is Result.Success<*> -> {
                    val success = it as Result.Success<LatLng>
                    val location = success.data
                    liveStatus.value = LocationStatus(location.latitude, location.longitude)
                }
                is Result.Error -> {
                    Log.e(TAG, "getRandomLocation: ", it.error)
                    liveMessage.value = R.string.random_error
                }
            }
        }
    }

    fun getCurrentLocation() {
        liveLoading.value = true
        fusedProvider.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            liveLoading.value = false
            if (locationResult != null) {
                val location = locationResult.locations.getOrNull(0)
                if ((location != null) && (location.hasAccuracy()) && (location.accuracy <= MIN_ACCURACY)) {
                    liveStatus.value = LocationStatus(location.latitude, location.longitude)
                }
            }
            fusedProvider.removeLocationUpdates(this)
        }
    }

    fun updateBearing(newBearing: Float) {
        liveStatus.value = liveStatus.value?.apply {
            bearing = newBearing
        }
    }

    fun updateLocation(location: LatLng) {
        liveStatus.value = liveStatus.value?.apply {
            latitude = location.latitude
            longitude = location.longitude
        }
    }

    fun searchLocation(query: String) {
        liveLoading.value = true
        getLocationFromQuery.invoke(Geocode.Request(query)) {
            liveLoading.value = false
            when (it) {
                is Result.Success<*> -> {
                    val success = it as Result.Success<LatLng>
                    val location = success.data
                    liveStatus.value = LocationStatus(location.latitude, location.longitude)
                }
                is Result.Error -> {
                    Log.e(TAG, "getLocationFromQuery: ", it.error)
                    liveMessage.value = R.string.geocoder_error
                }
            }
        }
    }
}