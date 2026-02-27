package com.example.grocerybudgetapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.grocerybudgetapp.R

object BudgetNotificationHelper {

    private const val CHANNEL_ID = "budget_alerts"
    private const val NOTIFICATION_ID_EXCEEDED = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_exceeded_title),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Budget and spending alerts" }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    fun showBudgetExceededNotification(context: Context, amountExceeded: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notification_exceeded_title))
            .setContentText(context.getString(R.string.notification_exceeded_text, amountExceeded))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID_EXCEEDED, notification)
    }
}
