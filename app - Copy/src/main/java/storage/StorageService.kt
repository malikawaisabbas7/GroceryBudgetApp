package com.example.grocerybudgetapp.storage

import android.content.Context
import com.example.grocerybudgetapp.GroceryItem
import com.example.grocerybudgetapp.ItemCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/** Local data storage for budget and grocery list (feature: Local Data Storage). */
class StorageService(private val context: Context) {

    private val prefs = context.getSharedPreferences("GroceryPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private  val KEY = "GroceryList"

    fun saveGroceryList(list: List<GroceryItem>?) {

        if (list == null) {
            prefs.edit().putString(KEY, "[]").apply()
            return
        }

        val json = gson.toJson(list)

        prefs.edit()
            .putString(KEY, json ?: "[]")
            .apply()
    }

    fun getGroceryList(): MutableList<GroceryItem> {
        val json = prefs.getString(KEY, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<GroceryItem>>() {}.type
            gson.fromJson(json, type)
        } else mutableListOf()
    }

    fun saveTotalBudget(amount: Double) {
        prefs.edit().putFloat("totalBudget", amount.toFloat()).apply()
    }

    fun getTotalBudget(): Double {
        return prefs.getFloat("totalBudget", 0f).toDouble()
    }

    fun getCategoryBudgets(): Map<String, Double> {
        val json = prefs.getString("categoryBudgets", null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    fun saveCategoryBudgets(map: Map<String, Double>) {
        prefs.edit().putString("categoryBudgets", gson.toJson(map)).apply()
    }
}
