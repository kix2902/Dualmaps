package com.redinput.dualmaps.data

import android.content.Context
import androidx.preference.PreferenceManager
import com.redinput.dualmaps.helpers.SingletonHolder

class PreferencesRepository private constructor(context: Context) : PreferencesSource {

    companion object : SingletonHolder<PreferencesRepository, Context>(::PreferencesRepository) {
        private const val KEY_GDPR_ENABLED = "gdpr-enabled"
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun loadFirebaseEnabled(): Boolean {
        return preferences.getBoolean(KEY_GDPR_ENABLED, false)
    }

    override fun saveFirebaseEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_GDPR_ENABLED, enabled).apply()
    }
}