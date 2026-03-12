@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.core.common

import com.example.hatchtracker.model.DeviceFeatures
import com.example.hatchtracker.model.IncubationLike
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Generates a day-by-day incubation timeline based on species profiles.
 */
object IncubationTimelineEngine {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun generateTimeline(incubation: IncubationLike, deviceFeatures: DeviceFeatures? = null): List<TimelineDay> {
        val profile = IncubationRegistry.getProfileForSpecies(incubation.species)
        val startDate = LocalDate.parse(incubation.startDate, dateFormatter)
        val totalDays = profile.incubationDurationDays
        val today = LocalDate.now()

        return (1..totalDays).map { day ->
            val currentDate = startDate.plusDays(day.toLong() - 1)
            val phase = when {
                day >= totalDays -> IncubationPhase.HATCH_WINDOW
                day >= profile.lockdownStartDay -> IncubationPhase.LOCKDOWN
                else -> IncubationPhase.EARLY_INCUBATION
            }

            val actions = mutableListOf<TimelineAction>()
            
            // Turning Logic
            if (profile.turning.required && day <= profile.turning.stopDay) {
                if (deviceFeatures?.autoTurn == true) {
                    // Auto-turn active: suppress manual turn task, maybe add info note?
                    // For now, let's just NOT add the critical task, effectively suppressing it.
                    // Ore maybe add a minor info note on Day 1 only.
                    if (day == 1) {
                         actions.add(TimelineAction(
                            title = "Auto-Turn Active",
                            description = "Your incubator is handling egg turning.",
                            isCritical = false
                        ))
                    }
                } else {
                    actions.add(TimelineAction(
                        title = "Turn Eggs",
                        description = "Turn eggs ${profile.turning.timesPerDay} times today to prevent sticking.",
                        isCritical = true,
                        notificationRuleId = if (day == 1) "turning_start" else null
                    ))
                }
            } else if (day == profile.turning.stopDay + 1) {
                // If auto-turn, message is to "Disable Auto-Turn"
                if (deviceFeatures?.autoTurn == true) {
                     actions.add(TimelineAction(
                        title = "Disable Auto-Turn",
                        description = "Remove the turning rack or disable the turning motor.",
                        isCritical = true,
                        notificationRuleId = "turning_stop"
                    ))
                } else {
                    actions.add(TimelineAction(
                        title = "Stop Turning",
                        description = "Remove turner or stop manual turning. Eggs must remain still.",
                        isCritical = true,
                        notificationRuleId = "turning_stop"
                    ))
                }
            }

            // Phase Transitions
            if (day == profile.lockdownStartDay) {
                actions.add(TimelineAction(
                    title = "Lockdown Start",
                    description = "Increase humidity and stop turning. Do not open the incubator!",
                    isCritical = true,
                    notificationRuleId = "lockdown_start"
                ))
                actions.add(TimelineAction(
                    title = "Humidity Check",
                    description = "Verify humidity is between ${profile.humidity.lockdownPhase.min}% and ${profile.humidity.lockdownPhase.max}%.",
                    isCritical = true,
                    notificationRuleId = "humidity_check_lockdown"
                ))
            }

            if (day == 1) {
                actions.add(TimelineAction(
                    title = "Settling Phase",
                    description = "Allow incubator to stabilize after setting eggs.",
                    isCritical = false
                ))
            }

            if (day == totalDays) {
                actions.add(TimelineAction(
                    title = "Hatch Day!",
                    description = "Estimated arrival of chicks. Prep the brooder.",
                    isCritical = true,
                    notificationRuleId = "hatch_window_start"
                ))
            }

            TimelineDay(
                dayNumber = day,
                date = currentDate,
                phase = phase,
                tempTarget = profile.temperature.optimal,
                humidityRange = if (phase == IncubationPhase.EARLY_INCUBATION) {
                    profile.humidity.earlyPhase.min to profile.humidity.earlyPhase.max
                } else {
                    profile.humidity.lockdownPhase.min to profile.humidity.lockdownPhase.max
                },
                actions = actions,
                isPast = currentDate.isBefore(today)
            )
        }
    }
}


