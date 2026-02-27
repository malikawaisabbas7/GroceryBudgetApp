package com.example.grocerybudgetapp.auth

import android.content.Context
import android.content.SharedPreferences

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

    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOGGED_IN, false)

    fun logout(context: Context) {
        prefs(context).edit()
            .remove(KEY_LOGGED_IN)
            .apply()
    }

    fun getCurrentUserName(context: Context): String =
        prefs(context).getString(KEY_NAME, null).orEmpty()
}
