package com.example.grocerybudgetapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.grocerybudgetapp.auth.AuthManager
import com.example.grocerybudgetapp.storage.StorageService
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
class BudgetActivity : AppCompatActivity() {

    private lateinit var etTotalBudget: TextInputEditText
    private lateinit var btnSaveBudget: MaterialButton

    private lateinit var monthSpinner: Spinner
    private lateinit var yearSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        // Views
        etTotalBudget = findViewById(R.id.etTotalBudget)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)

        monthSpinner = findViewById(R.id.spinnerMonth)
        yearSpinner = findViewById(R.id.spinnerYear)

        // Setup spinners
        setupSpinners()

        btnSaveBudget.setOnClickListener { saveBudget() }
        val btnViewHistory = findViewById<MaterialButton>(R.id.btnViewHistory)

        btnViewHistory.setOnClickListener {
            startActivity(Intent(this, BudgetHistoryActivity::class.java))
        }
    }

    private fun setupSpinners() {

        val months = arrayOf(
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        )

        val years = arrayOf(
            "2020",
            "2021",
            "2022",
            "2023",
            "2024",
            "2025",
            "2026",
            "2027",
            "2028",
            "2029",
            "2030"
        )

        monthSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)
        yearSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)

        // 🔥 AUTO SELECT CURRENT MONTH & YEAR
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        monthSpinner.setSelection(currentMonth)

        val yearIndex = years.indexOf(currentYear.toString())
        if (yearIndex != -1) {
            yearSpinner.setSelection(yearIndex)
        }
    }

    private fun saveBudget() {

        val budgetValue = etTotalBudget.text?.toString()?.toDoubleOrNull()

        if (budgetValue == null || budgetValue <= 0) {
            etTotalBudget.error = "Enter valid budget"
            return
        }

        val selectedMonth = monthSpinner.selectedItem.toString()
        val selectedYear = yearSpinner.selectedItem.toString()

        val sharedPref = getSharedPreferences("BUDGET_DATA", MODE_PRIVATE)

        val key = "budget_${selectedMonth}_${selectedYear}"

        sharedPref.edit()
            .putFloat(key, budgetValue.toFloat())
            .putString("last_month", selectedMonth)
            .putString("last_year", selectedYear)
            .putBoolean("budget_warning_shown", false)
            .apply()

        // ---------------- HISTORY SAVE ----------------

        val historyPref = getSharedPreferences("BUDGET_HISTORY", MODE_PRIVATE)
        val gson = Gson()

        val json = historyPref.getString("history_list", "[]")

        val type = object : TypeToken<MutableList<BudgetHistory>>() {}.type

        val list: MutableList<BudgetHistory> =
            gson.fromJson(json, type) ?: mutableListOf()

        val entry = BudgetHistory(
            selectedMonth,
            selectedYear,
            budgetValue,
            System.currentTimeMillis()
        )

        list.add(entry)

        historyPref.edit()
            .putString("history_list", gson.toJson(list))
            .apply()

        Snackbar.make(btnSaveBudget, "Budget Saved Successfully", Snackbar.LENGTH_SHORT).show()

        android.util.Log.d("HISTORY_SAVE", gson.toJson(list))

        setResult(Activity.RESULT_OK)
        finish()
    }
}