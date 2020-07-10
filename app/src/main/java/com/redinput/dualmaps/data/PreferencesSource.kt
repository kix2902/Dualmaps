package com.redinput.dualmaps.data

interface PreferencesSource {
    fun loadFirebaseEnabled(): Boolean
    fun saveFirebaseEnabled(enabled: Boolean)
    fun loadMapType(): Int
    fun loadShowCompass(): Boolean
    fun loadShowAddress(): Boolean
}