package com.example.grocerybudgetapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val dark = prefs.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (dark)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}