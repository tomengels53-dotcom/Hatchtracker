package com.example.hatchtracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.logging.SafeRun
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.core.common.IncubationManager
import com.example.hatchtracker.notifications.NotificationEngine
import com.example.hatchtracker.notifications.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HatchAlarmReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HatchAlarmReceiverEntryPoint {
        fun database(): AppDatabase
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.hatchtracker.ALARM_TRIGGER") return

        val incubationId = intent.getLongExtra("INCUBATION_ID", -1L)
        val type = intent.getStringExtra("ALARM_TYPE") ?: return

        if (incubationId == -1L) return

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val result = SafeRun.runSuspend(LogTags.NOTIFICATIONS, "HatchAlarmReceiver#onReceive") {
                val database = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    HatchAlarmReceiverEntryPoint::class.java
                ).database()
                val incubationDao = database.incubationDao()
                val historyDao = database.notificationHistoryDao()
                val inboxDao = database.inboxNotificationDao()
                val prefs = NotificationPreferences(context)
                
                val incubation = incubationDao.getIncubationEntityById(incubationId) ?: return@runSuspend
                val engine = NotificationEngine(historyDao, inboxDao, prefs, context.applicationContext)

                // Trigger specific logic based on type
                // Note: Engine already checks days logic, but for Alarms we force the specific check
                // Actually, we can just ask engine to run checks, and since the day matches, it SHOULD trigger.
                // But Engine deduplicates.
                
                // Or we force a notification helper call directly here?
                // Better to let Engine handle it so it records history and inbox.
                
                val events = engine.checkScheduledNotifications(incubation)
                
                events.forEach { event ->
                    // Since this is an Exact Alarm, likely Critical, we might want to ensure it shows
                    // irrespective of some engine dedup? No, Engine dedup is good.
                    
                    NotificationHelper.showGenericNotification(
                        context,
                        event.incubationId,
                        event.title,
                        event.message,
                        isCritical = true // Force high priority for these alarms
                    )
                    engine.recordNotificationSent(event.incubationId, event.ruleId)
                }
            }

            result.onFailure { throwable ->
                Logger.e(
                    LogTags.NOTIFICATIONS,
                    "op=HatchAlarmReceiver#onReceive incubationId=$incubationId alarmType=$type",
                    throwable
                )
            }
        }
    }
}
