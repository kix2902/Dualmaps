package com.redinput.dualmaps

import android.app.Application
import jonathanfinerty.once.Once

class DualmapsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Once.initialise(this)
    }
}