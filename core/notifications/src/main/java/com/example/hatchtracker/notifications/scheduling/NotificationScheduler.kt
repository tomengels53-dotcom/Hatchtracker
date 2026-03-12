package com.example.hatchtracker.notifications.scheduling

import android.content.Context
import androidx.work.*
import com.example.hatchtracker.data.models.IncubationLike
import com.example.hatchtracker.core.common.IncubationManager
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.notifications.workers.DailyIncubationWorker
import com.example.hatchtracker.notifications.workers.EnvironmentalMonitorWorker
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    /**
     * Schedules all necessary work for a new or restored incubation.
     */
    fun scheduleIncubation(context: Context, incubation: IncubationLike) {
        scheduleDailyCheck(context)
        scheduleEnvironmentalMonitor(context, incubation)
        
        // Also schedule exact alarms
        val alarmScheduler = AlarmScheduler(context)
        alarmScheduler.scheduleLockdownAlarm(incubation)
        alarmScheduler.scheduleHatchWindowAlarm(incubation)
        Logger.i(
            LogTags.NOTIFICATIONS,
            "op=scheduleIncubation status=updated incubationId=${incubation.id}"
        )
    }

    private fun scheduleDailyCheck(context: Context) {
        // Unique Periodic Work for Daily Check (runs once a day for ALL incubations)
        // We use KEEP so we don't overwrite if it already exists (assuming it covers all)
        // Actually, DailyWorker queries ALL active incubations, so one job is enough for the whole app.
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyRequest = PeriodicWorkRequestBuilder<DailyIncubationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "hatch_daily_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyRequest
        )
        Logger.i(
            LogTags.NOTIFICATIONS,
            "op=scheduleDailyCheck status=updated name=hatch_daily_check constraints=${dailyRequest.workSpec.constraints}"
        )
    }

    private fun scheduleEnvironmentalMonitor(context: Context, incubation: IncubationLike) {
        // This worker checks sensors. Frequency depends on phase.
        val config = IncubationManager.getConfig(incubation)
        val status = IncubationManager.getStatus(incubation)
        
        // If within 3 days of hatch (or in lockdown), check more frequently
        val daysUntilHatch = config.incubationDays - status.day
        val isCriticalPhase = daysUntilHatch <= 3
        
        val intervalHours = if (isCriticalPhase) 1L else 4L
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val monitorRequest = PeriodicWorkRequestBuilder<EnvironmentalMonitorWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("env_monitor_${incubation.id}")
            .build()
            
        // One worker per incubation? Or one global worker?
        // If we want adaptive frequency PER incubation, we need per-incubation workers OR a global one that runs fast and checks if it *needs* to check specific incubations.
        // A global worker running every 1h that checks "Is it time for incubation X?" is cleaner.
        // But for simplicity and complying with "Adaptive Logic" requirement per incubation:
        // WORK MANAGER MIN INTERVAL IS 15 MINUTES.
        // 1h vs 4h is fine.
        
        // Let's stick to Global Environmental Worker for simplicity and battery, triggered every 2h hours (compromise).
        // Or specific tag?
        // Implementation plan said "scheduleEnvironmentalWorker(incubation)".
        // Let's do a Global Monitor that runs every 2 hours. It's simpler.
        // Adaptivity can be handled inside the worker: "If not critical, skip every other check"?
        // No, let's just run it every 3 hours globally.
        
        // REVISION: The prompt asked for "Adaptive Logic".
        // "If incubation day >= (hatchDay - 3): Increase monitoring frequency"
        // To strictly support this with WorkManager, we'd need separate jobs or update the global one.
        // Let's try separate unique work per incubation if we want distinct frequencies, but that scales poorly.
        // Better: One Global Worker running every 1 hour.
        // Inside doWork():
        //   For each incubation:
        //     If critical: Check now.
        //     If not critical: Check only if (currentHour % 4 == 0).
        // That is robust.
        
        val globalMonitorRequest = PeriodicWorkRequestBuilder<EnvironmentalMonitorWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "hatch_env_monitor",
            ExistingPeriodicWorkPolicy.UPDATE, // Update policy to ensure we keep it running
            globalMonitorRequest
        )
        Logger.i(
            LogTags.NOTIFICATIONS,
            "op=scheduleEnvironmentalMonitor status=updated name=hatch_env_monitor incubationId=${incubation.id}"
        )
    }
    
    fun rescheduleAll(context: Context, incubations: List<IncubationLike>) {
        // Enqueue global workers
        scheduleDailyCheck(context)
        
        // Enqueue global monitor (just once needed)
        val constraints = Constraints.Builder().setRequiresBatteryNotLow(true).build()
        val globalMonitorRequest = PeriodicWorkRequestBuilder<EnvironmentalMonitorWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "hatch_env_monitor",
            ExistingPeriodicWorkPolicy.UPDATE,
            globalMonitorRequest
        )
        Logger.i(
            LogTags.NOTIFICATIONS,
            "op=rescheduleAll status=updated name=hatch_env_monitor incubations=${incubations.size}"
        )
        
        // Reschedule exact alarms for each
        val alarmScheduler = AlarmScheduler(context)
        incubations.forEach { 
             alarmScheduler.scheduleLockdownAlarm(it)
             alarmScheduler.scheduleHatchWindowAlarm(it)
             com.example.hatchtracker.NotificationHelper.scheduleAllTimelineNotifications(context, it)
        }
        Logger.i(
            LogTags.NOTIFICATIONS,
            "op=rescheduleAll status=updated alarms=${incubations.size}"
        )
    }

    fun cancelAllForIncubation(context: Context, incubationId: Long) {
        // If we had per-incubation workers, cancel them here.
        // Since we moved to global, we just cancel alarms.
        val alarmScheduler = AlarmScheduler(context)
        alarmScheduler.cancelAlarms(incubationId)
        
        // Notification Center Cleanup?
        // Maybe remove scheduled notifications if any (not supported by this architecture yet other than DB history)
    }
}




