package com.example.ecozaschitnik.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.ecozaschitnik.R
import com.google.android.material.appbar.MaterialToolbar

object ThemeManager {

    private const val PREFS = "eco_theme_prefs"
    private const val KEY_MODE = "theme_mode"

    fun applyStoredTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(readMode(context))
    }

    fun toggle(activity: AppCompatActivity) {
        val next = if (isNight(activity)) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MODE, next)
            .apply()
        AppCompatDelegate.setDefaultNightMode(next)
        activity.recreate()
    }

    fun isNight(context: Context): Boolean {
        val mode = readMode(context)
        if (mode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            return mode == AppCompatDelegate.MODE_NIGHT_YES
        }
        val nightFlags = context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    fun updateToolbarThemeIcon(toolbar: MaterialToolbar, context: Context) {
        toolbar.menu.findItem(R.id.action_theme)?.setIcon(
            if (isNight(context)) R.drawable.ic_theme_sun else R.drawable.ic_theme_moon
        )
    }

    private fun readMode(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_YES)
    }
}
