@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.notifications.scheduling

import android.content.Context
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.core.common.IncubationTimelineEngine
import com.example.hatchtracker.core.common.NurseryConfig
import com.example.hatchtracker.data.models.Flocklet
import com.example.hatchtracker.data.models.IncubationLike
import com.example.hatchtracker.notifications.NotificationPreferences
import com.example.hatchtracker.notifications.ReminderCategory
import com.example.hatchtracker.core.notifications.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HatchyReminderScheduler(private val context: Context) {
    private val prefs = NotificationPreferences(context)

    suspend fun checkAndScheduleReminders(
        incubations: List<IncubationLike>,
        flocklets: List<Flocklet>
    ) {
        if (!prefs.isNotificationsEnabled) return

        // 1. Incubation Reminders
        if (prefs.isIncubationRemindersEnabled && prefs.canSendReminderToday(ReminderCategory.INCUBATION)) {
            checkIncubationReminders(incubations)
        }

        // 2. Nursery Reminders
        if (prefs.isNurseryRemindersEnabled && prefs.canSendReminderToday(ReminderCategory.NURSERY)) {
            checkNurseryReminders(flocklets)
        }

        // 3. Flock Reminders
        if (prefs.isFlockRemindersEnabled && prefs.canSendReminderToday(ReminderCategory.FLOCK)) {
            checkFlockReminders(flocklets)
        }
    }

    private fun checkIncubationReminders(incubations: List<IncubationLike>) {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        incubations.filter { !it.hatchCompleted }.forEach { incubation ->
            val timeline = IncubationTimelineEngine.generateTimeline(incubation)
            
            // Lockdown Reminder
            val lockdownDay = timeline.find { it.actions.any { action -> action.notificationRuleId == "lockdown_start" } }
            if (lockdownDay?.date == tomorrow) {
                val reminderId = "lockdown_${incubation.id}_${lockdownDay.date}"
                if (!prefs.isReminderDismissed(reminderId)) {
                    sendHatchyNotification(
                        id = incubation.id,
                        category = ReminderCategory.INCUBATION,
                        title = context.getString(R.string.hatchy_reminders_title),
                        message = context.getString(R.string.hatchy_lockdown_reminder)
                    )
                    prefs.markReminderSent(ReminderCategory.INCUBATION)
                    return // Max 1 per category per day
                }
            }

            // Hatch Window Reminder
            val hatchDay = timeline.find { it.actions.any { action -> action.notificationRuleId == "hatch_window_start" } }
            if (hatchDay?.date == tomorrow) {
                val reminderId = "hatch_${incubation.id}_${hatchDay.date}"
                if (!prefs.isReminderDismissed(reminderId)) {
                    sendHatchyNotification(
                        id = incubation.id,
                        category = ReminderCategory.INCUBATION,
                        title = context.getString(R.string.hatchy_reminders_title),
                        message = context.getString(R.string.hatchy_hatch_window_reminder)
                    )
                    prefs.markReminderSent(ReminderCategory.INCUBATION)
                    return // Max 1 per category per day
                }
            }
        }
    }

    private fun checkNurseryReminders(flocklets: List<Flocklet>) {
        val today = LocalDate.now()
        
        flocklets.filter { it.movedToFlockId == null }.forEach { flocklet ->
            val hatchDate = java.time.Instant.ofEpochMilli(flocklet.hatchDate)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val ageInDays = ChronoUnit.DAYS.between(hatchDate, today).toInt()
            
            // Brooder temperature reminder (trigger weekly for variety, or every day if preferred)
            // Requirements say "Brooder temperature reminders by age"
            // We'll trigger it if it hasn't been sent today (handled by caller)
            val rule = NurseryConfig.getRuleForSpecies(flocklet.species)
            val targetTempC = rule.initialTemp - (ageInDays * rule.tempReductionPerDay)
            val finalTempC = if (targetTempC < rule.minSurvivalTemp) rule.minSurvivalTemp else targetTempC
            val finalTempF = (finalTempC * 1.8 + 32).toInt()

            val reminderId = "nursery_temp_${flocklet.id}_$ageInDays"
            if (!prefs.isReminderDismissed(reminderId)) {
                sendHatchyNotification(
                    id = flocklet.id,
                    category = ReminderCategory.NURSERY,
                    title = context.getString(R.string.hatchy_reminders_title),
                    message = context.getString(R.string.hatchy_brooder_temp_reminder, ageInDays, finalTempF)
                )
                prefs.markReminderSent(ReminderCategory.NURSERY)
                return // Max 1 per category per day
            }
        }
    }

    private fun checkFlockReminders(flocklets: List<Flocklet>) {
        val today = LocalDate.now()

        flocklets.filter { it.movedToFlockId == null }.forEach { flocklet ->
            val rule = NurseryConfig.getRuleForSpecies(flocklet.species)
            val hatchDate = java.time.Instant.ofEpochMilli(flocklet.hatchDate)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val ageInDays = ChronoUnit.DAYS.between(hatchDate, today).toInt()

            if (ageInDays >= rule.minAgeForFlock) {
                val reminderId = "flock_ready_${flocklet.id}"
                if (!prefs.isReminderDismissed(reminderId)) {
                    sendHatchyNotification(
                        id = flocklet.id,
                        category = ReminderCategory.FLOCK,
                        title = context.getString(R.string.hatchy_reminders_title),
                        message = context.getString(R.string.hatchy_flock_ready_reminder)
                    )
                    prefs.markReminderSent(ReminderCategory.FLOCK)
                    return // Max 1 per category per day
                }
            }
        }
    }

    private fun sendHatchyNotification(id: Long, category: ReminderCategory, title: String, message: String) {
        // We can reuse showGenericNotification or create a specific Hatchy one
        NotificationHelper.showGenericNotification(
            context = context,
            incubationId = id,
            title = title,
            message = message,
            isCritical = false
        )
    }
}
