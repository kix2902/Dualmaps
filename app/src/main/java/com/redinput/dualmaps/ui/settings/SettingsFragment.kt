package com.redinput.dualmaps.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.redinput.dualmaps.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }
}