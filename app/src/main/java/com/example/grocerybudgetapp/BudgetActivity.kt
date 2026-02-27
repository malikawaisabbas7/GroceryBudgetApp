package com.example.grocerybudgetapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class BudgetActivity : AppCompatActivity() {

    private lateinit var etTotalBudget: TextInputEditText
    private lateinit var btnSaveBudget: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        etTotalBudget = findViewById(R.id.etTotalBudget)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)

        btnSaveBudget.setOnClickListener { saveBudget() }
    }

    private fun saveBudget() {
        val budget = etTotalBudget.text?.toString()?.toDoubleOrNull() ?: 0.0
        setResult(Activity.RESULT_OK, Intent().putExtra("totalBudget", budget))
        finish()
    }
}
