package com.shejan.financebuddy.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shejan.financebuddy.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE_REMINDER = "com.shejan.financebuddy.action.FIRE_DAILY_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val preferencesManager = PreferencesManager(context)
                val isEnabled = preferencesManager.isDailyReminderEnabled.first()

                when (intent.action) {
                    ACTION_FIRE_REMINDER -> {
                        if (isEnabled) {
                            DailyReminderManager.showReminderNotification(context)
                            // Re-arm alarm for the next day at the same configured time
                            val hour = preferencesManager.dailyReminderHour.first()
                            val minute = preferencesManager.dailyReminderMinute.first()
                            DailyReminderManager.scheduleDailyReminder(context, hour, minute)
                        }
                    }
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    "android.intent.action.QUICKBOOT_POWERON",
                    "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                        if (isEnabled) {
                            val hour = preferencesManager.dailyReminderHour.first()
                            val minute = preferencesManager.dailyReminderMinute.first()
                            DailyReminderManager.scheduleDailyReminder(context, hour, minute)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
