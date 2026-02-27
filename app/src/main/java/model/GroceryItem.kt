package com.example.grocerybudgetapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Grocery item with quantity, total cost, payment mode, and category.
 * Unit price is computed as price / quantity.
 */
@Parcelize
data class GroceryItem(
    val name: String,
    val price: Double,
    val quantity: Double = 1.0,
    val paymentMode: String = PaymentMode.CASH,
    val category: String = ItemCategory.FOOD,
    val id: String = "",
    val addedAt: Long = System.currentTimeMillis()
) : Parcelable {


    val unitPrice: Double
        get() = if (quantity > 0) price / quantity else 0.0

    fun withId(newId: String) = copy(id = newId)
}

object PaymentMode {
    const val CASH = "Cash"
    const val CARD = "Card"
    const val EASYPAISA = "Easypaisa"
    const val JAZZCASH = "JazzCash"
    val ALL = listOf(CASH, CARD, EASYPAISA, JAZZCASH)
}

object ItemCategory {
    const val FOOD = "Food"
    const val BEVERAGES = "Beverages"
    const val HOUSEHOLD = "Household"
    const val PERSONAL_CARE = "Personal care"
    val ALL = listOf(FOOD, BEVERAGES, HOUSEHOLD, PERSONAL_CARE)
}