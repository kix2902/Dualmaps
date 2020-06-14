package com.redinput.dualmaps.domain

import com.redinput.dualmaps.data.PreferencesSource
import kotlinx.coroutines.CoroutineScope

class LoadGDPR(scope: CoroutineScope, private val repository: PreferencesSource) :
    UseCase<Boolean, UseCase.None>(scope) {

    override suspend fun run(params: None): Result {
        val enabled = repository.loadFirebaseEnabled()
        return Result.Success(enabled)
    }
}