package com.dearly.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DearlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
