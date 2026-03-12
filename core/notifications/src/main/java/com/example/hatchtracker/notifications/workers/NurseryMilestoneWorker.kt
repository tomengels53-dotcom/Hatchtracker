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

class NurseryMilestoneWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val flockletId = inputData.getLong("flockletId", -1L)
        val milestoneType = inputData.getString("milestoneType") ?: "nursery"
        val workerName = javaClass.simpleName
        val result = SafeRun.run(LogTags.NOTIFICATIONS, "NurseryMilestoneWorker#doWork") {
            val prefs = NotificationPreferences(applicationContext)
            if (!prefs.isNurseryRemindersEnabled) return@run
            val title = inputData.getString("title")
                ?: applicationContext.getString(R.string.notification_nursery_reminder_title)
            val message = inputData.getString("message") ?: ""
            val isCritical = inputData.getBoolean("isCritical", false)

            if (flockletId != -1L) {
                NotificationHelper.showGenericNotification(
                    context,
                    flockletId,
                    title,
                    message,
                    isCritical
                )
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
                    "op=NurseryMilestoneWorker#doWork flockletId=$flockletId milestone=$milestoneType",
                    throwable
                )
                WorkerRunTracker.recordFailure(applicationContext, workerName, throwable)
                if (SafeRun.isTransient(throwable)) Result.retry() else Result.failure()
            }
        )
    }
}
