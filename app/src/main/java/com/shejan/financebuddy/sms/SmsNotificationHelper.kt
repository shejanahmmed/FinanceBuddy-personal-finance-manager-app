package com.shejan.financebuddy.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shejan.financebuddy.MainActivity
import com.shejan.financebuddy.R
import java.text.DecimalFormat

object SmsNotificationHelper {

    private const val CHANNEL_ID = "transaction_inbox"
    private const val CHANNEL_NAME = "Transaction Alerts"
    private const val CHANNEL_DESC = "Notifications for newly detected bank and mobile banking transactions"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun notifyNewTransaction(
        context: Context,
        amount: Double,
        accountName: String,
        type: String
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PENDING_INBOX", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formatter = DecimalFormat("##,##,##0.00")
        val formattedAmount = "৳${formatter.format(amount)}"
        val typeLabel = when (type.uppercase()) {
            "INCOME" -> "Income received"
            "TRANSFER" -> "Transfer detected"
            else -> "Expense detected"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.financebuddy)
            .setContentTitle("New Transaction: $formattedAmount")
            .setContentText("$typeLabel in $accountName. Tap to review & confirm.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }
}
