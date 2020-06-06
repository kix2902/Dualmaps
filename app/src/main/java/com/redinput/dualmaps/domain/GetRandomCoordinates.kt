package com.redinput.dualmaps.domain

import com.google.android.gms.maps.model.LatLng
import com.redinput.dualmaps.data.NetworkSource
import com.redinput.dualmaps.domain.UseCase
import kotlinx.coroutines.CoroutineScope

class GetRandomCoordinates(scope: CoroutineScope, private val repository: NetworkSource) :
    UseCase<LatLng, UseCase.None>(scope) {

    override suspend fun run(params: None): Result {
        try {
            val response = repository.getRandomLocation()
            val location = LatLng(response.nearest.latt, response.nearest.longt)
            return Result.Success(location)

        } catch (ex: Exception) {
            return Result.Error(ex)
        }
    }

}