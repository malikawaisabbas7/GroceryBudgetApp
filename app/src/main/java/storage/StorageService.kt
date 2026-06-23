package com.example.grocerybudgetapp.storage

import android.content.Context
import com.example.grocerybudgetapp.GroceryItem
import com.example.grocerybudgetapp.ItemCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StorageService(private val context: Context, private val email: String) {

    private val prefs = context.getSharedPreferences(
        "GroceryPrefs_$email",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()
    private val KEY = "GroceryList"

    // ===================== GROCERY LIST =====================

    fun saveGroceryList(list: List<GroceryItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY, json).apply()
    }

    fun getGroceryList(): MutableList<GroceryItem> {
        val json = prefs.getString(KEY, null)

        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<GroceryItem>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } else {
            mutableListOf()
        }
    }

    // ===================== BUDGET =====================



    // ===================== CATEGORIES (ADD HERE 🔥) =====================

    fun getCategories(): MutableList<String> {
        val set = prefs.getStringSet("categories", null)

        return if (set.isNullOrEmpty()) {
            mutableListOf(
                ItemCategory.FOOD,
                ItemCategory.BEVERAGES,
                ItemCategory.HOUSEHOLD,
                ItemCategory.PERSONAL_CARE
            )
        } else {
            set.toMutableList()
        }
    }

    fun saveCategories(list: List<String>) {
        prefs.edit()
            .putStringSet("categories", list.toSet())
            .apply()
    }
    fun saveMonthlyBudget(monthYear: String, amount: Double) {
        prefs.edit()
            .putFloat("budget_$monthYear", amount.toFloat())
            .apply()
    }

    fun getMonthlyBudget(monthYear: String): Double {
        return prefs.getFloat("budget_$monthYear", 0f).toDouble()
    }

    fun getAllBudgets(): Map<String, Double> {

        val allPrefs = prefs.all

        val result = mutableMapOf<String, Double>()

        for ((key, value) in allPrefs) {
            if (key.startsWith("budget_")) {
                val monthYear = key.removePrefix("budget_")
                result[monthYear] = (value as Float).toDouble()
            }
        }

        return result
    }
    // ===================== CLEAR =====================

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}