package com.redinput.dualmaps.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.redinput.dualmaps.R
import com.redinput.dualmaps.hasLocationPermission

class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: SettingsViewModel by activityViewModels()

    private var prefLocationPermission: SwitchPreferenceCompat? = null
    private var prefGdpr: SwitchPreferenceCompat? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefLocationPermission = findPreference(getString(R.string.key_location_permission))
        prefLocationPermission?.setOnPreferenceChangeListener { _, _ ->
            showDialogPermission()
            true
        }

        prefGdpr = findPreference(getString(R.string.key_gdpr))
        prefGdpr?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.updateGdpr(newValue as Boolean)
            true
        }

        viewModel.liveGdpr.observe(viewLifecycleOwner, Observer {
            prefGdpr?.isChecked = it
        })
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadGdpr()
        prefLocationPermission?.isChecked = requireContext().hasLocationPermission()
    }

    private fun showDialogPermission() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_change_permission_title)
            .setMessage(R.string.dialog_change_permission_message)
            .setPositiveButton(R.string.dialog_change_permission_positive) { _, _ ->
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", requireContext().packageName, null)
                })
            }
            .setNegativeButton(R.string.dialog_change_permission_negative) { _, _ ->
                prefLocationPermission?.isChecked = requireContext().hasLocationPermission()
            }
            .show()
    }
}