package com.example.grocerybudgetapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.grocerybudgetapp.auth.AuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etOtp: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnUpdatePassword: MaterialButton

    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        email = intent.getStringExtra("email").orEmpty()

        etOtp = findViewById(R.id.etOtp)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword)

        btnUpdatePassword.setOnClickListener { updatePassword() }
    }

    private fun updatePassword() {
        val otp = etOtp.text?.toString()?.trim().orEmpty()
        val newPass = etNewPassword.text?.toString().orEmpty()
        val confirm = etConfirmPassword.text?.toString().orEmpty()

        if (otp.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.error_fill_all),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        if (!AuthManager.verifyResetOtp(this, email, otp)) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.invalid_or_expired_otp),
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction("Resend") {
                    val newOtp = AuthManager.startPasswordReset(this@ResetPasswordActivity, email)
                    if (newOtp != null) {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            getString(R.string.otp_sent_simulated, newOtp),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
                .show()
            return
        }
        if (newPass.length < 6) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.password_rules),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        if (newPass != confirm) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.passwords_do_not_match),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        if (!AuthManager.updatePassword(this, email, newPass)) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.email_not_registered),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.password_reset_success),
            Snackbar.LENGTH_LONG
        ).show()
        val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
        sharedPref.edit()
            .putBoolean("logged_in", true)
            .putString("logged_in_email", email)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

