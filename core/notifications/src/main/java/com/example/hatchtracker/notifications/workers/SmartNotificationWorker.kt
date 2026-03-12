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
import com.example.hatchtracker.notifications.NotificationEngine
import com.example.hatchtracker.notifications.NotificationPreferences
import com.example.hatchtracker.notifications.NotificationSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker

@HiltWorker
class SmartNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: com.example.hatchtracker.data.AppDatabase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var currentIncubationId: Long? = null
        val workerName = javaClass.simpleName
        val result = SafeRun.runSuspend(LogTags.NOTIFICATIONS, "SmartNotificationWorker#doWork") {
            val historyDao = database.notificationHistoryDao()
            val inboxDao = database.inboxNotificationDao()
            val incubationDao = database.incubationDao()

            val prefs = NotificationPreferences(applicationContext)
            if (!prefs.isIncubationRemindersEnabled) return@runSuspend
            val engine = NotificationEngine(historyDao, inboxDao, prefs, applicationContext)
            
            // Get all active incubations
            val incubations = incubationDao.getAllIncubationEntitys().filter { !it.hatchCompleted }
            
            for (incubation in incubations) {
                currentIncubationId = incubation.id
                // 1. Scheduled Checks
                val scheduledEvents = engine.checkScheduledNotifications(incubation)
                
                for (event in scheduledEvents) {
                    NotificationHelper.showGenericNotification(
                        applicationContext,
                        event.incubationId,
                        event.title,
                        event.message,
                        isCritical = event.severity == NotificationSeverity.CRITICAL
                    )
                    engine.recordNotificationSent(event.incubationId, event.ruleId)
                }

                // 2. Environmental Checks (Mocked for now as we don't have sensor data)
                // In future, fetch latest sensor reading here
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
                    "op=SmartNotificationWorker#doWork incubationId=${currentIncubationId ?: "unknown"}",
                    throwable
                )
                WorkerRunTracker.recordFailure(applicationContext, workerName, throwable)
                if (SafeRun.isTransient(throwable)) Result.retry() else Result.failure()
            }
        )
    }
}
