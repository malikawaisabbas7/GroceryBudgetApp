package com.example.grocerybudgetapp

data class BudgetHistory(
    val month: String,
    val year: String,
    val amount: Double,
    val timestamp: Long
)