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
import com.redinput.dualmaps.*
import com.redinput.dualmaps.data.GeocoderRepository
import com.redinput.dualmaps.data.NetworkRepository
import com.redinput.dualmaps.data.PreferencesRepository
import com.redinput.dualmaps.domain.*
import com.redinput.dualmaps.domain.UseCase.None
import com.redinput.dualmaps.domain.UseCase.Result.Error
import com.redinput.dualmaps.domain.UseCase.Result.Success

@SuppressLint("MissingPermission")
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val MIN_ACCURACY = 100
    }

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
    private val getAddressFromLocation = GetAddressFromLocation(
        viewModelScope,
        GeocoderRepository.getInstance(application.applicationContext)
    )
    private val loadMapType = LoadMapType(
        viewModelScope,
        PreferencesRepository.getInstance(application.applicationContext)
    )
    private val loadShowCompass = LoadShowCompass(
        viewModelScope,
        PreferencesRepository.getInstance(application.applicationContext)
    )
    private val loadShowAddress = LoadShowAddress(
        viewModelScope,
        PreferencesRepository.getInstance(application.applicationContext)
    )

    private val _liveLocationStatus = MutableLiveData<LocationStatus?>(null)
    val liveLocationStatus: LiveData<LocationStatus?> = _liveLocationStatus

    private val _liveLoading = MutableLiveData(false)
    val liveLoading: LiveData<Boolean> = _liveLoading

    private val _liveMessage = LiveEvent<Message>()
    val liveMessage: LiveData<Message> = _liveMessage

    private val _liveUIStatus: MutableLiveData<UIStatus>
    val liveUIStatus: LiveData<UIStatus>

    init {
        val defaultMapType = application.getString(R.string.default_map_type).toInt()
        val defaultShowCompass = application.resources.getBoolean(R.bool.default_show_compass)
        val defaultShowAddress = application.resources.getBoolean(R.bool.default_show_address)
        _liveUIStatus =
            MutableLiveData(UIStatus(defaultMapType, defaultShowCompass, defaultShowAddress))
        liveUIStatus = _liveUIStatus
    }

    fun updateUI() {
        loadMapType.invoke(None()) {
            val newMapType = (it as Success<Int>).data
            _liveUIStatus.value = _liveUIStatus.value?.apply {
                mapType = newMapType
            }
        }
        loadShowCompass.invoke(None()) {
            val newShowCompass = (it as Success<Boolean>).data
            _liveUIStatus.value = _liveUIStatus.value?.apply {
                showCompass = newShowCompass
            }
        }
        loadShowAddress.invoke(None()) {
            val newShowAddress = (it as Success<Boolean>).data
            _liveUIStatus.value = _liveUIStatus.value?.apply {
                showAddress = newShowAddress
            }
        }
    }

    fun getRandomLocation() {
        _liveLoading.value = true
        getRandomCoordinates.invoke(None()) {
            _liveLoading.value = false
            when (it) {
                is Success<*> -> {
                    val success = it as Success<LatLng>
                    val location = success.data
                    _liveLocationStatus.value =
                        LocationStatus(location.latitude, location.longitude)
                    refreshAddress()
                }
                is Error -> {
                    Log.e(TAG, "getRandomLocation: ", it.error)
                    _liveMessage.value = Message(MessageType.ERROR, R.string.random_error)
                }
            }
        }
    }

    fun getCurrentLocation() {
        _liveLoading.value = true
        fusedProvider.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            _liveLoading.value = false
            if (locationResult != null) {
                val location = locationResult.locations.getOrNull(0)
                if ((location != null) && (location.hasAccuracy()) && (location.accuracy <= MIN_ACCURACY)) {
                    _liveLocationStatus.value =
                        LocationStatus(location.latitude, location.longitude)
                    refreshAddress()
                }
            }
            fusedProvider.removeLocationUpdates(this)
        }
    }

    fun updateBearing(newBearing: Float) {
        _liveLocationStatus.value = _liveLocationStatus.value?.apply {
            bearing = newBearing
        }
    }

    fun updateLocation(location: LatLng) {
        _liveLocationStatus.value = _liveLocationStatus.value?.apply {
            latitude = location.latitude
            longitude = location.longitude
        }
        refreshAddress()
    }

    private fun refreshAddress() {
        _liveLocationStatus.value?.let { status ->
            val location = LatLng(status.latitude, status.longitude)
            getAddressFromLocation.invoke(location) {
                when (it) {
                    is Success<*> -> {
                        val success = it as Success<Geocode.Address>
                        val address = success.data
                        _liveLocationStatus.value = _liveLocationStatus.value?.apply {
                            this.address = Address(address.title, address.subtitle)
                        }
                    }
                    is Error -> {
                        Log.e(TAG, "getAddressFromLocation: ", it.error)
                        _liveLocationStatus.value = _liveLocationStatus.value?.apply {
                            this.address = null
                        }
                    }
                }
            }
        }
    }

    fun searchLocation(query: String) {
        _liveLoading.value = true
        getLocationFromQuery.invoke(Geocode.Request(query)) {
            _liveLoading.value = false
            when (it) {
                is Success<*> -> {
                    val success = it as Success<LatLng>
                    val location = success.data
                    _liveLocationStatus.value =
                        LocationStatus(location.latitude, location.longitude)
                }
                is Error -> {
                    Log.e(TAG, "getLocationFromQuery: ", it.error)
                    _liveMessage.value = Message(MessageType.ERROR, R.string.geocoder_error)
                }
            }
        }
    }
}