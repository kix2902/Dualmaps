package com.redinput.dualmaps.domain

import com.redinput.dualmaps.data.PreferencesSource
import kotlinx.coroutines.CoroutineScope

class LoadMapType(scope: CoroutineScope, private val repository: PreferencesSource) :
    UseCase<Int, UseCase.None>(scope) {

    override suspend fun run(params: None): Result {
        val mapType = repository.loadMapType()
        return Result.Success(mapType)
    }
}