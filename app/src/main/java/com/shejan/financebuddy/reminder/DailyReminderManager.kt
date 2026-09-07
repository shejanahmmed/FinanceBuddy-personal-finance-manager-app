package com.shejan.financebuddy.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shejan.financebuddy.MainActivity
import com.shejan.financebuddy.R
import com.shejan.financebuddy.data.PreferencesManager
import kotlinx.coroutines.flow.first
import java.util.Calendar

object DailyReminderManager {

    const val CHANNEL_ID = "daily_finance_reminder"
    private const val CHANNEL_NAME = "Daily Finance Reminders"
    private const val CHANNEL_DESC = "Daily reminder notifications to record and update your expenses and income"
    const val EXTRA_SLOT_INDEX = "extra_reminder_slot_index"

    private const val BASE_REQUEST_CODE = 8800
    private const val BASE_NOTIFICATION_ID = 8900

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

    /**
     * Schedules a daily alarm for a specific slot index (1, 2, or 3) at [hour] (0-23) and [minute] (0-59).
     */
    fun scheduleSlotReminder(context: Context, slotIndex: Int, hour: Int, minute: Int) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = BASE_REQUEST_CODE + slotIndex

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_FIRE_REMINDER
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val targetCalendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the scheduled time for today has already passed, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerTime = targetCalendar.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for strict permission environments
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * Cancels a pending daily reminder alarm for the given [slotIndex].
     */
    fun cancelSlotReminder(context: Context, slotIndex: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = BASE_REQUEST_CODE + slotIndex

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_FIRE_REMINDER
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Cancels all 3 daily reminder slot alarms.
     */
    fun cancelAllReminders(context: Context) {
        for (slot in 1..3) {
            cancelSlotReminder(context, slot)
        }
    }

    /**
     * Reschedules all enabled reminder slots based on user preferences.
     */
    suspend fun rescheduleAllActiveReminders(context: Context) {
        val preferencesManager = PreferencesManager(context)
        val isMasterEnabled = preferencesManager.isDailyReminderEnabled.first()

        if (!isMasterEnabled) {
            cancelAllReminders(context)
            return
        }

        // Slot 1
        if (preferencesManager.reminder1Enabled.first()) {
            val h1 = preferencesManager.reminder1Hour.first()
            val m1 = preferencesManager.reminder1Minute.first()
            scheduleSlotReminder(context, 1, h1, m1)
        } else {
            cancelSlotReminder(context, 1)
        }

        // Slot 2
        if (preferencesManager.reminder2Enabled.first()) {
            val h2 = preferencesManager.reminder2Hour.first()
            val m2 = preferencesManager.reminder2Minute.first()
            scheduleSlotReminder(context, 2, h2, m2)
        } else {
            cancelSlotReminder(context, 2)
        }

        // Slot 3
        if (preferencesManager.reminder3Enabled.first()) {
            val h3 = preferencesManager.reminder3Hour.first()
            val m3 = preferencesManager.reminder3Minute.first()
            scheduleSlotReminder(context, 3, h3, m3)
        } else {
            cancelSlotReminder(context, 3)
        }
    }

    /** Legacy helper for slot 1 / primary reminder */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        scheduleSlotReminder(context, 1, hour, minute)
    }

    /** Legacy helper to cancel all reminders */
    fun cancelDailyReminder(context: Context) {
        cancelAllReminders(context)
    }

    /**
     * Displays the daily finance check-in notification for a specific [slotIndex].
     */
    fun showReminderNotification(context: Context, slotIndex: Int = 1) {
        createNotificationChannel(context)

        val notificationId = BASE_NOTIFICATION_ID + slotIndex

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, body) = when (slotIndex) {
            1 -> Pair(
                "Daily Finance Reminder 💰",
                "Don't forget to record today's expenses & income to keep your balance accurate."
            )
            2 -> Pair(
                "Midday Expense Check ☀️",
                "Take a moment to record any lunch or afternoon expenses from today."
            )
            3 -> Pair(
                "Morning Finance Review 🌅",
                "Start your day prepared—check your budget and track any scheduled payments."
            )
            else -> Pair(
                "Daily Finance Reminder 💰",
                "Don't forget to record today's expenses & income."
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.financebuddy)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notification)
    }

    /**
     * Formats an hour (0-23) and minute (0-59) into a 12-hour AM/PM string (e.g., "08:00 PM").
     */
    fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(calendar.time)
    }
}
