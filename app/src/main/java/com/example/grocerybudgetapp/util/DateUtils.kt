package com.example.grocerybudgetapp.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}