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
                val isMasterEnabled = preferencesManager.isDailyReminderEnabled.first()

                when (intent.action) {
                    ACTION_FIRE_REMINDER -> {
                        if (isMasterEnabled) {
                            val slotIndex = intent.getIntExtra(DailyReminderManager.EXTRA_SLOT_INDEX, 1)
                            val (isSlotEnabled, hour, minute) = when (slotIndex) {
                                1 -> Triple(
                                    preferencesManager.reminder1Enabled.first(),
                                    preferencesManager.reminder1Hour.first(),
                                    preferencesManager.reminder1Minute.first()
                                )
                                2 -> Triple(
                                    preferencesManager.reminder2Enabled.first(),
                                    preferencesManager.reminder2Hour.first(),
                                    preferencesManager.reminder2Minute.first()
                                )
                                3 -> Triple(
                                    preferencesManager.reminder3Enabled.first(),
                                    preferencesManager.reminder3Hour.first(),
                                    preferencesManager.reminder3Minute.first()
                                )
                                else -> Triple(false, 20, 0)
                            }

                            if (isSlotEnabled) {
                                DailyReminderManager.showReminderNotification(context, slotIndex)
                                // Re-arm alarm for the next day at the same configured slot time
                                DailyReminderManager.scheduleSlotReminder(context, slotIndex, hour, minute)
                            }
                        }
                    }
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    "android.intent.action.QUICKBOOT_POWERON",
                    "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                        DailyReminderManager.rescheduleAllActiveReminders(context)
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
