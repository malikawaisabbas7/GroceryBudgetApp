package com.example.grocerybudgetapp.auth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import com.example.grocerybudgetapp.R

/**
 * Local authentication (User Registration/Login).
 * Credentials stored locally per feature list.
 */
object AuthManager {

    private const val PREFS_NAME = "AuthPrefs"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"
    private const val KEY_PASSWORD = "password"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_RESET_OTP = "reset_otp"
    private const val KEY_RESET_OTP_EXPIRES_AT = "reset_otp_expires_at"
    private const val KEY_RESET_EMAIL = "reset_email"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun register(context: Context, name: String, email: String, password: String) {
        prefs(context).edit()
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    fun isEmailRegistered(context: Context, email: String): Boolean {
        val savedEmail = prefs(context).getString(KEY_EMAIL, null) ?: return false
        return email.trim().equals(savedEmail, ignoreCase = true)
    }

    fun login(context: Context, email: String, password: String): Boolean {
        val p = prefs(context)
        val savedEmail = p.getString(KEY_EMAIL, null) ?: return false
        val savedPassword = p.getString(KEY_PASSWORD, null) ?: return false
        if (email.trim().equals(savedEmail, ignoreCase = true) && password == savedPassword) {
            p.edit().putBoolean(KEY_LOGGED_IN, true).apply()
            return true
        }
        return false
    }

    /**
     * Starts password reset and returns an OTP.
     * This app is offline/local, so this OTP should be shown in-app (simulated email).
     */
    fun startPasswordReset(context: Context, email: String): String? {
        if (!isEmailRegistered(context, email)) return null
        val otp = (100000..999999).random().toString()

// 🔥 ADD THIS LINE
        showOtpNotification(context, otp)
        val expiresAt = System.currentTimeMillis() + 10 * 60 * 1000 // 10 minutes
        prefs(context).edit()
            .putString(KEY_RESET_EMAIL, email.trim())
            .putString(KEY_RESET_OTP, otp)
            .putLong(KEY_RESET_OTP_EXPIRES_AT, expiresAt)
            .apply()
        return otp
    }

    fun verifyResetOtp(context: Context, email: String, otp: String): Boolean {
        val p = prefs(context)
        val savedEmail = p.getString(KEY_RESET_EMAIL, null) ?: return false
        val savedOtp = p.getString(KEY_RESET_OTP, null) ?: return false
        val expiresAt = p.getLong(KEY_RESET_OTP_EXPIRES_AT, 0L)
        if (!email.trim().equals(savedEmail, ignoreCase = true)) return false
        if (System.currentTimeMillis() > expiresAt) return false
        return otp.trim() == savedOtp
    }

    fun updatePassword(context: Context, email: String, newPassword: String): Boolean {
        if (!isEmailRegistered(context, email)) return false
        prefs(context).edit()
            .putString(KEY_PASSWORD, newPassword)
            .remove(KEY_RESET_EMAIL)
            .remove(KEY_RESET_OTP)
            .remove(KEY_RESET_OTP_EXPIRES_AT)
            .apply()
        return true
    }

    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOGGED_IN, false)

    fun logout(context: Context) {
        prefs(context).edit()
            .remove(KEY_LOGGED_IN)
            .apply()
    }

    fun getCurrentUserName(context: Context): String =
        prefs(context).getString(KEY_NAME, null).orEmpty()
    fun getCurrentUserEmail(context: Context): String =
        prefs(context).getString(KEY_EMAIL, "").orEmpty()
}
private fun showOtpNotification(context: Context, otp: String) {

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channelId = "otp_channel"

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "OTP Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

}