package com.redinput.dualmaps.data

import android.content.Context
import androidx.preference.PreferenceManager
import com.redinput.dualmaps.R
import com.redinput.dualmaps.helpers.SingletonHolder

class PreferencesRepository private constructor(context: Context) : PreferencesSource {

    companion object : SingletonHolder<PreferencesRepository, Context>(::PreferencesRepository) {
        private const val KEY_GDPR_ENABLED = "gdpr-enabled"
    }

    private val KEY_MAP_TYPE = context.getString(R.string.key_map_type)
    private val DEFAULT_MAP_TYPE = context.getString(R.string.default_map_type)

    private val KEY_SHOW_COMPASS = context.getString(R.string.key_show_compass)
    private val DEFAULT_SHOW_COMPASS = context.resources.getBoolean(R.bool.default_show_compass)

    private val KEY_SHOW_ADDRESS = context.getString(R.string.key_show_address)
    private val DEFAULT_SHOW_ADDRESS = context.resources.getBoolean(R.bool.default_show_address)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun loadFirebaseEnabled(): Boolean {
        return preferences.getBoolean(KEY_GDPR_ENABLED, false)
    }

    override fun saveFirebaseEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_GDPR_ENABLED, enabled).apply()
    }

    override fun loadMapType(): Int {
        return preferences.getString(KEY_MAP_TYPE, DEFAULT_MAP_TYPE)!!.toInt()
    }

    override fun loadShowCompass(): Boolean {
        return preferences.getBoolean(KEY_SHOW_COMPASS, DEFAULT_SHOW_COMPASS)
    }

    override fun loadShowAddress(): Boolean {
        return preferences.getBoolean(KEY_SHOW_ADDRESS, DEFAULT_SHOW_ADDRESS)
    }
}