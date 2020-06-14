package com.redinput.dualmaps.ui.onboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.redinput.dualmaps.Onboard
import com.redinput.dualmaps.OnboardState
import com.redinput.dualmaps.R
import com.redinput.dualmaps.data.PreferencesRepository
import com.redinput.dualmaps.domain.SaveGDPR
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class OnboardViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val GDPR = "gdpr"
    }

    private val context = application.applicationContext

    private val moshi = Moshi.Builder().build()
    private val onboardJsonType =
        Types.newParameterizedType(List::class.java, Onboard.Step::class.java)
    private val adapterOnboard = moshi.adapter<List<Onboard.Step>>(onboardJsonType)

    private lateinit var steps: List<Onboard.Step>

    private val status = OnboardState()
    private val liveStatus = MutableLiveData(status)
    fun getObservableStatus(): LiveData<OnboardState> = liveStatus

    private val saveGDPR = SaveGDPR(viewModelScope, PreferencesRepository.getInstance(context))

    fun loadOnboardFile() {
        val json = context.resources
            .openRawResource(R.raw.onboard)
            .bufferedReader()
            .use { it.readText() }

        steps = adapterOnboard.fromJson(json)!!

        liveStatus.value = liveStatus.value?.also {
            it.total = steps.size
            it.step = steps.getOrNull(it.position)
        }
    }

    fun savePreferenceBoolean(key: String, value: Boolean) {
        if (key == GDPR) {
            saveGDPR.invoke(value)
        }
    }

    fun nextStep() {
        liveStatus.value = liveStatus.value?.also {
            it.position++
            it.step = steps.getOrNull(it.position)
        }
    }
}
