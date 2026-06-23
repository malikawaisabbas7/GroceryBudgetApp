package com.example.grocerybudgetapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.grocerybudgetapp.auth.AuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import androidx.core.app.NotificationCompat

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSendOtp: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        etEmail = findViewById(R.id.etEmail)
        btnSendOtp = findViewById(R.id.btnSendOtp)

        btnSendOtp.setOnClickListener { sendOtp() }
    }

    private fun sendOtp() {
        val email = etEmail.text?.toString()?.trim().orEmpty()

        if (email.isEmpty()) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.error_fill_all),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        // ✅ STEP 1: OTP generated from AuthManager
        val otp = AuthManager.startPasswordReset(this, email)

        if (otp == null) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.email_not_registered),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "otp_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "OTP Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)

            .setContentText("Your OTP is: $otp")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(false)
            .build()

        notificationManager.notify(1, notification)
        // ✅ STEP 2: SAVE OTP LOCALLY (ADD THIS HERE)
        val sharedPref = getSharedPreferences("OTP_PREF", MODE_PRIVATE)
        sharedPref.edit().putString("otp", otp).apply()

        // ✅ STEP 3: SHOW MESSAGE (demo mode)
        Snackbar.make(
            findViewById(android.R.id.content),
            "OTP sent successfully (Demo): $otp",
            Snackbar.LENGTH_LONG
        ).show()

        // ❌ REMOVE this line if you don't want duplicate OTP system:
        // AuthManager already handles logic

        // ✅ STEP 4: MOVE TO NEXT SCREEN
        startActivity(Intent(this, ResetPasswordActivity::class.java).apply {
            putExtra("email", email)
        })

        finish()
    }
}

