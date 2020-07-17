package com.redinput.dualmaps.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.redinput.dualmaps.DualmapsApplication
import com.redinput.dualmaps.data.PreferencesRepository
import com.redinput.dualmaps.domain.LoadGDPR
import com.redinput.dualmaps.domain.SaveGDPR
import com.redinput.dualmaps.domain.UseCase
import com.redinput.dualmaps.domain.UseCase.Result.Success

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val loadGDPR = LoadGDPR(
        viewModelScope,
        PreferencesRepository.getInstance(application.applicationContext)
    )
    private val saveGDPR = SaveGDPR(
        viewModelScope,
        PreferencesRepository.getInstance(application.applicationContext)
    )

    private val _liveGdpr = MutableLiveData(false)
    val liveGdpr: LiveData<Boolean> = _liveGdpr

    fun loadGdpr() {
        loadGDPR.invoke(
            UseCase.None(),
            onResult = {
                val enabled = (it as Success<Boolean>).data
                _liveGdpr.value = enabled
            }
        )
    }

    fun updateGdpr(value: Boolean) {
        saveGDPR.invoke(value)
        FirebaseAnalytics.getInstance(getApplication<DualmapsApplication>().applicationContext)
            .setAnalyticsCollectionEnabled(value)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(value)
    }
}