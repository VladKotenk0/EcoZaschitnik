package com.example.ecozaschitnik

import android.app.Application
import com.example.ecozaschitnik.ui.ThemeManager

class EcoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyStoredTheme(this)
    }
}
