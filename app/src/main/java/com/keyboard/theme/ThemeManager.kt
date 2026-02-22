package com.keyboard.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    fun apply(context: Context) {
        val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
        when (prefs.getString("mode", "system")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
