package com.example.hatchtracker.notifications.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.logging.SafeRun
import com.example.hatchtracker.core.logging.WorkerRunTracker
import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.core.common.IncubationManager
import com.example.hatchtracker.notifications.NotificationEngine
import com.example.hatchtracker.notifications.NotificationPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker

@HiltWorker
class DailyIncubationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var currentIncubationId: Long? = null
        val workerName = javaClass.simpleName
        val result = SafeRun.runSuspend(
            LogTags.NOTIFICATIONS,
            "DailyIncubationWorker#doWork"
        ) {
            val incubationDao = database.incubationDao()
            val historyDao = database.notificationHistoryDao()
            val inboxDao = database.inboxNotificationDao()
            val prefs = NotificationPreferences(applicationContext)
            if (!prefs.isIncubationRemindersEnabled) return@runSuspend
            
            val engine = NotificationEngine(historyDao, inboxDao, prefs, applicationContext)

            val incubations = incubationDao.getAllIncubationEntitys().filter { !it.hatchCompleted }

            for (incubation in incubations) {
                currentIncubationId = incubation.id
                // Determine day logic could go here, but IncubationManager calculates it dynamically from startDate
                
                // Check scheduled notifications (Lockdown warnings etc not handled by AlarmManager)
                // AlarmManager handles EXACT lockdown start/hatch start. 
                // But daily worker checks "Day before lockdown", "Day before hatch", etc.
                
                val events = engine.checkScheduledNotifications(incubation)
                events.forEach { event ->
                    NotificationHelper.showGenericNotification(
                        applicationContext,
                        event.incubationId,
                        event.title,
                        event.message,
                        isCritical = false // Daily checks usually warnings/info
                    )
                    engine.recordNotificationSent(event.incubationId, event.ruleId)
                }
            }
        }

        result.fold(
            onSuccess = {
                WorkerRunTracker.recordSuccess(applicationContext, workerName)
                Result.success()
            },
            onFailure = { throwable ->
                Logger.e(
                    LogTags.NOTIFICATIONS,
                    "op=DailyIncubationWorker#doWork incubationId=${currentIncubationId ?: "unknown"}",
                    throwable
                )
                WorkerRunTracker.recordFailure(applicationContext, workerName, throwable)
                if (SafeRun.isTransient(throwable)) Result.retry() else Result.failure()
            }
        )
    }
}
