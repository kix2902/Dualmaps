package com.redinput.dualmaps.domain

import android.content.res.Resources
import com.google.android.gms.maps.model.LatLng
import com.redinput.dualmaps.Geocode
import com.redinput.dualmaps.data.GeocoderRepository
import kotlinx.coroutines.CoroutineScope

class GetLocationFromQuery(scope: CoroutineScope, private val repository: GeocoderRepository) :
    UseCase<LatLng, Geocode.Request>(scope) {

    override suspend fun run(params: Geocode.Request): Result {
        try {
            val response = repository.getLocationFromName(params.query)
            if (response == null) {
                return Result.Error(Resources.NotFoundException("Location not found for this query"))
            }
            val location = LatLng(response.latitude, response.longitude)
            return Result.Success(location)

        } catch (ex: Exception) {
            return Result.Error(ex)
        }
    }

}