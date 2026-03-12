package com.example.hatchtracker.notifications.workers
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.logging.SafeRun
import com.example.hatchtracker.core.logging.WorkerRunTracker
import com.example.hatchtracker.notifications.NotificationPreferences
import com.example.hatchtracker.core.notifications.R
class TimelineMilestoneWorker(
    val context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val incubationId = inputData.getLong("incubationId", -1L)
        val milestoneType = inputData.getString("milestoneType") ?: "timeline"
        val workerName = javaClass.simpleName
        val result = SafeRun.run(LogTags.NOTIFICATIONS, "TimelineMilestoneWorker#doWork") {
            val prefs = NotificationPreferences(applicationContext)
            if (!prefs.isIncubationRemindersEnabled) return@run
            val title = inputData.getString("title")
                ?: applicationContext.getString(R.string.notification_incubation_reminder_title)
            val message = inputData.getString("message") ?: ""
            val isCritical = inputData.getBoolean("isCritical", false)
            if (incubationId != -1L) {
                NotificationHelper.showGenericNotification(context, incubationId, title, message, isCritical)
            }
        }

        return result.fold(
            onSuccess = {
                WorkerRunTracker.recordSuccess(applicationContext, workerName)
                Result.success()
            },
            onFailure = { throwable ->
                Logger.e(
                    LogTags.NOTIFICATIONS,
                    "op=TimelineMilestoneWorker#doWork incubationId=$incubationId milestone=$milestoneType",
                    throwable
                )
                WorkerRunTracker.recordFailure(applicationContext, workerName, throwable)
                if (SafeRun.isTransient(throwable)) Result.retry() else Result.failure()
            }
        )
    }
}


