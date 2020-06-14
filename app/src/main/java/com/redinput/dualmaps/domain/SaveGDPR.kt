package com.redinput.dualmaps.domain

import com.redinput.dualmaps.data.PreferencesSource
import kotlinx.coroutines.CoroutineScope

class SaveGDPR(scope: CoroutineScope, private val repository: PreferencesSource) :
    UseCase<UseCase.None, Boolean>(scope) {

    override suspend fun run(params: Boolean): Result {
        repository.saveFirebaseEnabled(params)
        return Result.Success(None())
    }
}