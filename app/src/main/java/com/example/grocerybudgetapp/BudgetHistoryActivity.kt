package com.example.grocerybudgetapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BudgetHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_history)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val historyPref = getSharedPreferences("BUDGET_HISTORY", MODE_PRIVATE)
        val json = historyPref.getString("history_list", "[]")

        val gson = Gson()

        val type = object : TypeToken<MutableList<BudgetHistory>>() {}.type

        val historyList: List<BudgetHistory> = gson.fromJson(json, type)

        recyclerView.adapter = BudgetHistoryAdapter(historyList)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }
    }
}