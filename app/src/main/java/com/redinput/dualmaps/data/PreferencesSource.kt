package com.redinput.dualmaps.data

interface PreferencesSource {
    fun loadFirebaseEnabled(): Boolean
    fun saveFirebaseEnabled(enabled: Boolean)
}