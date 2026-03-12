package com.example.hatchtracker.notifications.workers
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.logging.SafeRun
import com.example.hatchtracker.core.logging.WorkerRunTracker
import com.example.hatchtracker.notifications.NotificationPreferences
import com.example.hatchtracker.core.notifications.R
class HatchNotificationWorker(
    context: android.content.Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val incubationId = inputData.getInt("incubationId", -1)
        val milestoneType = inputData.getString("milestoneType") ?: "hatch_reminder"
        val workerName = javaClass.simpleName
        val result = SafeRun.run(LogTags.NOTIFICATIONS, "HatchNotificationWorker#doWork") {
            val prefs = NotificationPreferences(applicationContext)
            if (!prefs.isIncubationRemindersEnabled) return@run
            val birdName = inputData.getString("birdName")
                ?: applicationContext.getString(R.string.notification_unknown_bird)
            val expectedHatch = inputData.getString("expectedHatch") ?: ""
            NotificationHelper.showNotification(
                applicationContext,
                incubationId,
                birdName,
                expectedHatch
            )
        }

        return result.fold(
            onSuccess = {
                WorkerRunTracker.recordSuccess(applicationContext, workerName)
                Result.success()
            },
            onFailure = { throwable ->
                Logger.e(
                    LogTags.NOTIFICATIONS,
                    "op=HatchNotificationWorker#doWork incubationId=$incubationId milestone=$milestoneType",
                    throwable
                )
                WorkerRunTracker.recordFailure(applicationContext, workerName, throwable)
                if (SafeRun.isTransient(throwable)) Result.retry() else Result.failure()
            }
        )
    }
}


