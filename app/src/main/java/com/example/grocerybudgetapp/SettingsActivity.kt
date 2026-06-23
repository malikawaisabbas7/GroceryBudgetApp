package com.example.grocerybudgetapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.grocerybudgetapp.auth.AuthManager
import com.example.grocerybudgetapp.storage.StorageService
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val email = AuthManager.getCurrentUserEmail(this)

        if (email.isBlank()) {
            AuthManager.logout(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val name = AuthManager.getCurrentUserName(this)
        val storage = StorageService(this, email)

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        tvName.text = name
        tvEmail.text = email

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val switchDarkMode = findViewById<SwitchMaterial>(R.id.switchDarkMode)

// load saved value
        val isDark = prefs.getBoolean("dark_mode", false)

// apply theme
        AppCompatDelegate.setDefaultNightMode(
            if (isDark)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

// set switch state safely
        switchDarkMode.setOnCheckedChangeListener(null)
        switchDarkMode.isChecked = isDark

        switchDarkMode.setOnCheckedChangeListener { _, checked ->

            prefs.edit().putBoolean("dark_mode", checked).apply()

            AppCompatDelegate.setDefaultNightMode(
                if (checked)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        findViewById<View>(R.id.btnClearData)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("Delete all groceries and budget?")
                .setPositiveButton("Yes") { _, _ ->
                    storage.clearAllData()
                    Toast.makeText(this, "Data cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }

        findViewById<View>(R.id.btnPdf)?.setOnClickListener {
            startActivity(Intent(this, PdfActivity::class.java))
        }

        findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            AuthManager.logout(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }


        findViewById<View>(R.id.btnAbout)?.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        val toolbar =
            findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

}