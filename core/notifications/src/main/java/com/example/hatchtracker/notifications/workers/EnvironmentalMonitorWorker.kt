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
import com.example.hatchtracker.notifications.NotificationSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker

@HiltWorker
class EnvironmentalMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: com.example.hatchtracker.data.AppDatabase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var currentIncubationId: Long? = null
        val workerName = javaClass.simpleName
        val result = SafeRun.runSuspend(LogTags.NOTIFICATIONS, "EnvironmentalMonitorWorker#doWork") {
            val incubationDao = database.incubationDao()
            val historyDao = database.notificationHistoryDao()
            val inboxDao = database.inboxNotificationDao()
            val prefs = NotificationPreferences(applicationContext)

            val engine = NotificationEngine(historyDao, inboxDao, prefs, applicationContext)

            val incubations = incubationDao.getAllIncubationEntitys().filter { !it.hatchCompleted }

            for (incubation in incubations) {
                currentIncubationId = incubation.id
                // Mock Sensor Data (Simulating a device reading)
                // In production, this would call a bluetooth service or API
                
                // Let's pretend we have a sensor reading:
                // We'll trust the User's last input? No, that's not monitoring.
                // For now, initiate checks assuming we *had* data. 
                // Since we don't have real hardware, we'll skip the actual check unless
                // we want to simulate random failures for demo purposes.
                
                // Let's skip mocking random failures to avoid annoying the user.
                // But we WILL enable the structure so it's ready for hardware integration.
                
                /*
                val currentTemp = SensorManager.getTemp(incubation.id)
                val currentHumidity = SensorManager.getHumidity(incubation.id)
                
                val events = engine.checkEnvironmentalNotifications(incubation, currentTemp, currentHumidity)
                events.forEach { event ->
                    NotificationHelper.showGenericNotification(
                        applicationContext,
                        event.incubationId,
                        event.title,
                        event.message,
                        isCritical = event.severity == NotificationSeverity.CRITICAL
                    )
                    engine.recordNotificationSent(event.incubationId, event.ruleId)
                }
                */
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
                    "op=EnvironmentalMonitorWorker#doWork incubationId=${currentIncubationId ?: "unknown"}",
                    throwable
                )
                WorkerRunTracker.recordFailure(applicationContext, workerName, throwable)
                if (SafeRun.isTransient(throwable)) Result.retry() else Result.failure()
            }
        )
    }
}

