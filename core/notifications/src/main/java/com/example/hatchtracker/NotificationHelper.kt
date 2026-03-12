@file:android.annotation.SuppressLint("NewApi", "MissingPermission")

package com.example.hatchtracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.notifications.workers.HatchNotificationWorker
import com.example.hatchtracker.notifications.workers.HatchyReminderWorker
import com.example.hatchtracker.notifications.workers.NurseryMilestoneWorker
import com.example.hatchtracker.notifications.workers.SmartNotificationWorker
import com.example.hatchtracker.notifications.workers.TimelineMilestoneWorker
import com.example.hatchtracker.notifications.NotificationPreferences
import com.example.hatchtracker.data.models.Flocklet
import com.example.hatchtracker.core.common.NurseryConfig
import com.example.hatchtracker.core.notifications.R
import com.example.hatchtracker.core.ui.R as UiR
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.time.LocalTime
import java.time.ZoneId

/**
 * Helper class for managing hatch notifications
 */
object NotificationHelper {
    private const val CHANNEL_ID = "hatch_notifications"

    /**
     * Creates the notification channel (required for Android 8.0+)
     * Call this in Application.onCreate() or MainActivity.onCreate()
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules a notification for 1 day before the expected hatch date
     *
     * @param context Application context
     * @param incubationId Unique ID for the incubation
     * @param birdName Name of the parent bird
     * @param expectedHatch Expected hatch date in YYYY-MM-DD format
     */
    fun scheduleHatchNotification(
        context: Context,
        incubationId: Int,
        birdName: String,
        expectedHatch: String
    ) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val hatchDate = LocalDate.parse(expectedHatch, formatter)
            val notificationDate = hatchDate.minusDays(1) // 1 day before
            val today = LocalDate.now()

            // Only schedule if notification date is in the future
            if (notificationDate.isBefore(today)) {
                // If notification date has passed, don't schedule
                Logger.i(
                    LogTags.NOTIFICATIONS,
                    "op=scheduleHatchNotification status=skipped incubationId=$incubationId reason=past_date"
                )
                return
            }

            // Calculate delay in milliseconds
            val delayMillis = java.time.Duration.between(
                today.atStartOfDay(),
                notificationDate.atStartOfDay()
            ).toMillis()

            // Create work request
            val workRequest = OneTimeWorkRequestBuilder<HatchNotificationWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        "incubationId" to incubationId,
                        "milestoneType" to "hatch_reminder",
                        "birdName" to birdName,
                        "expectedHatch" to expectedHatch
                    )
                )
                .addTag("hatch_notification_$incubationId")
                .build()

            // Enqueue the work (idempotent)
            WorkManager.getInstance(context).enqueueUniqueWork(
                "hatch_notification_$incubationId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            Logger.i(
                LogTags.NOTIFICATIONS,
                "op=scheduleHatchNotification status=replaced incubationId=$incubationId"
            )
        } catch (e: Exception) {
            // Handle date parsing errors
            e.printStackTrace()
        }
    }

    /**
     * Cancels a scheduled notification for a specific incubation
     */
    fun cancelHatchNotification(context: Context, incubationId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag("hatch_notification_$incubationId")
    }

    /**
     * Shows the notification immediately (used by the Worker)
     */
    fun showNotification(
        context: Context,
        incubationId: Int,
        birdName: String,
        expectedHatch: String
    ) {
        if (!canDeliverNotification(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(UiR.drawable.hatchy_1)
            .setContentTitle(context.getString(R.string.notification_hatch_alert_title))
            .setContentText(context.getString(R.string.notification_hatch_alert_text, birdName))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_hatch_alert_big_text, birdName, expectedHatch))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(incubationId, notification)
        }
    }

    /**
     * Schedules periodic checks for smart notifications
     */
    fun schedulePeriodicChecks(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<SmartNotificationWorker>(
            6, TimeUnit.HOURS, // Check every 6 hours
            1, TimeUnit.HOURS  // 1 hour flex window
        )
            .addTag("smart_incubation_checks")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "smart_incubation_checks",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        Logger.i(
            LogTags.NOTIFICATIONS,
            "op=schedulePeriodicChecks status=updated name=smart_incubation_checks constraints=${workRequest.workSpec.constraints}"
        )
    }

    /**
     * Schedules periodic checks for Hatchy reminders
     */
    fun scheduleHatchyReminders(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<HatchyReminderWorker>(
            12, TimeUnit.HOURS, // Check twice a day
            1, TimeUnit.HOURS  // 1 hour flex window
        )
            .addTag("hatchy_reminder_checks")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "hatchy_reminder_checks",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        Logger.i(
            LogTags.NOTIFICATIONS,
            "op=scheduleHatchyReminders status=updated name=hatchy_reminder_checks constraints=${workRequest.workSpec.constraints}"
        )
    }

    /**
     * Cancels all scheduled notifications for a specific incubation
     */
    fun cancelAllIncubationNotifications(context: Context, incubationId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("hatch_notification_$incubationId")
        // Also cancel all milestone-specific tags
        WorkManager.getInstance(context).cancelAllWorkByTag("milestone_$incubationId")

        // Note: Using a prefix tag "milestone_$incubationId" works if we also add it as a general tag.
        // Let's ensure we add it in scheduleAllTimelineNotifications.
    }

    /**
     * Schedules all critical milestones for an incubation
     */
    fun scheduleAllTimelineNotifications(context: Context, incubation: com.example.hatchtracker.data.models.IncubationLike) {
        val timeline = com.example.hatchtracker.core.common.IncubationTimelineEngine.generateTimeline(incubation)
        val today = java.time.LocalDate.now()

        timeline.forEach { day ->
            // Only schedule future milestones
            if (day.date.isAfter(today) || (day.date.isEqual(today) && java.time.LocalTime.now().isBefore(java.time.LocalTime.of(8, 0)))) {
                day.actions.forEach { action ->
                    val ruleId = action.notificationRuleId ?: return@forEach

                    // Schedule for 8:00 AM on the target day
                    val targetDateTime = day.date.atTime(java.time.LocalTime.of(8, 0))
                    val delayMillis = java.time.Duration.between(java.time.LocalDateTime.now(), targetDateTime).toMillis()

                    if (delayMillis > 0) {
                        val workRequest = OneTimeWorkRequestBuilder<TimelineMilestoneWorker>()
                            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                            .setInputData(
                                workDataOf(
                                    "incubationId" to incubation.id,
                                    "milestoneType" to ruleId,
                                    "title" to action.title,
                                    "message" to "${incubation.species}: ${action.description}",
                                    "isCritical" to action.isCritical
                                )
                            )
                            .addTag("milestone_${incubation.id}")
                            .addTag("milestone_${incubation.id}_$ruleId")
                            .build()

                        WorkManager.getInstance(context).enqueueUniqueWork(
                            "milestone_${incubation.id}_$ruleId",
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                        Logger.i(
                            LogTags.NOTIFICATIONS,
                            "op=scheduleTimeline status=replaced incubationId=${incubation.id} milestone=$ruleId"
                        )
                    } else {
                        Logger.i(
                            LogTags.NOTIFICATIONS,
                            "op=scheduleTimeline status=skipped incubationId=${incubation.id} milestone=$ruleId reason=delay<=0"
                        )
                    }
                }
            }
        }
    }

    fun showGenericNotification(
        context: Context,
        incubationId: Long,
        title: String,
        message: String,
        isCritical: Boolean = false,
        deeplink: String? = null
    ) {
        if (!canDeliverNotification(context)) return

        val priority = if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        
        val pendingIntent = deeplink?.let { link ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                `package` = context.packageName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            PendingIntent.getActivity(
                context, 
                link.hashCode(), 
                intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(UiR.drawable.hatchy_1)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setAutoCancel(true)
        
        pendingIntent?.let { builder.setContentIntent(it) }

        with(NotificationManagerCompat.from(context)) {
            notify(incubationId.toInt() + title.hashCode(), builder.build())
        }
    }

    private fun canDeliverNotification(context: Context): Boolean {
        val prefs = NotificationPreferences(context)
        if (!prefs.isNotificationsEnabled) {
            Logger.i(LogTags.NOTIFICATIONS, "op=deliverNotification status=skipped reason=global_disabled")
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Logger.i(LogTags.NOTIFICATIONS, "op=deliverNotification status=skipped reason=permission_denied")
            return false
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Logger.i(LogTags.NOTIFICATIONS, "op=deliverNotification status=skipped reason=notifications_disabled_system")
            return false
        }
        return true
    }

    /**
     * Schedules nursery milestone notifications (EXPERT/PRO only).
     */
    fun scheduleNurseryMilestones(
        context: Context,
        flocklet: Flocklet,
        canSchedule: Boolean
    ) {
        if (!canSchedule) {
            Logger.i(
                LogTags.NOTIFICATIONS,
                "op=scheduleNurseryMilestones status=skipped flockletId=${flocklet.id} reason=not_allowed"
            )
            return
        }

        val zoneId = ZoneId.systemDefault()
        val hatchDate = java.time.Instant.ofEpochMilli(flocklet.hatchDate).atZone(zoneId).toLocalDate()
        val rule = NurseryConfig.getRuleForSpecies(flocklet.species)

        scheduleNurseryMilestone(
            context = context,
            flockletId = flocklet.id,
            hatchDate = hatchDate,
            daysAfterHatch = 14,
            milestoneId = "week_2_check",
            title = context.getString(R.string.notification_week2_title),
            message = context.getString(R.string.notification_week2_message, flocklet.species)
        )

        scheduleNurseryMilestone(
            context = context,
            flockletId = flocklet.id,
            hatchDate = hatchDate,
            daysAfterHatch = rule.minAgeForFlock,
            milestoneId = "grow_out_ready",
            title = context.getString(R.string.notification_growout_title),
            message = context.getString(R.string.notification_growout_message, flocklet.species),
            isCritical = true
        )
    }

    fun cancelNurseryMilestones(context: Context, flockletId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("nursery_${flockletId}")
    }

    private fun scheduleNurseryMilestone(
        context: Context,
        flockletId: Long,
        hatchDate: LocalDate,
        daysAfterHatch: Int,
        milestoneId: String,
        title: String,
        message: String,
        isCritical: Boolean = false
    ) {
        val targetDate = hatchDate.plusDays(daysAfterHatch.toLong())
        val targetDateTime = targetDate.atTime(LocalTime.of(8, 0))
        val now = java.time.LocalDateTime.now()
        val delayMillis = java.time.Duration.between(now, targetDateTime).toMillis()

        if (delayMillis <= 0) {
            Logger.i(
                LogTags.NOTIFICATIONS,
                "op=scheduleNurseryMilestone status=skipped flockletId=$flockletId milestone=$milestoneId reason=delay<=0"
            )
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<NurseryMilestoneWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "flockletId" to flockletId,
                    "milestoneType" to milestoneId,
                    "title" to title,
                    "message" to message,
                    "isCritical" to isCritical
                )
            )
            .addTag("nursery_${flockletId}")
            .addTag("nursery_${flockletId}_$milestoneId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "nursery_${flockletId}_$milestoneId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Logger.i(
            LogTags.NOTIFICATIONS,
            "op=scheduleNurseryMilestone status=replaced flockletId=$flockletId milestone=$milestoneId"
        )
    }
}
