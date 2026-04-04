package com.arijit.budgettracker.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arijit.budgettracker.MainActivity
import com.arijit.budgettracker.R
import com.arijit.budgettracker.utils.CurrencyPrefs
import java.text.NumberFormat
import java.util.Locale

object SmsNotificationHelper {

    private const val CHANNEL_ID = "sms_transactions"
    private const val CHANNEL_NAME = "SMS Transactions"
    private val notificationId = java.util.concurrent.atomic.AtomicInteger(1000)

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for auto-detected bank transactions"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showTransactionNotification(
        context: Context,
        amount: Double,
        type: String,
        category: String,
        bankName: String
    ) {
        val currencySymbol = CurrencyPrefs.getSymbol(context)
        val formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(amount)

        val title: String
        val text: String

        if (type == "INCOME") {
            title = "Nhận $currencySymbol$formattedAmount"
            text = "Từ $bankName"
        } else {
            title = "Chi $currencySymbol$formattedAmount"
            text = "$category - $bankName"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("OPEN_TAB", 1)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val currentNotifId = notificationId.getAndIncrement()

        val pendingIntent = PendingIntent.getActivity(
            context, currentNotifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.app_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(currentNotifId, notification)
    }
}
