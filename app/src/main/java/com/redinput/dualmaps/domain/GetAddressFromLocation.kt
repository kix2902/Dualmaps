package com.redinput.dualmaps.domain

import android.content.res.Resources
import com.google.android.gms.maps.model.LatLng
import com.redinput.dualmaps.Geocode
import com.redinput.dualmaps.data.GeocoderRepository
import kotlinx.coroutines.CoroutineScope

class GetAddressFromLocation(scope: CoroutineScope, private val repository: GeocoderRepository) :
    UseCase<Geocode.Address, LatLng>(scope) {

    override suspend fun run(params: LatLng): Result {
        try {
            val response = repository.getAddressFromLocation(params)
            if (response == null) {
                return Result.Error(Resources.NotFoundException("Location hasn't an address associated"))
            }
            return Result.Success(response)

        } catch (ex: Exception) {
            return Result.Error(ex)
        }
    }

}