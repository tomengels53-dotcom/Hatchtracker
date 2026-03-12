@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.core.common.IncubationManager
import com.example.hatchtracker.core.common.IncubationPhase
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.NotificationHistory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Enhanced Models for Timeline
enum class TimelineStatus {
    COMPLETED,
    TODAY,
    UPCOMING,
    WARNING,
    CRITICAL
}

data class TimelineEvent(
    val title: String,
    val description: String,
    val isCritical: Boolean = false,
    val isCompleted: Boolean = false,
    val actionLabel: String? = null
)

// Note: Using TimelineDayView to avoid conflict with TimelineDayState in IncubationManager.kt
data class TimelineDayView(
    val dayNumber: Int,
    val date: LocalDate,
    val status: TimelineStatus,
    val events: List<TimelineEvent>,
    val phase: IncubationPhase
)

object TimelineGenerator {
    
    fun generate(
        incubation: Incubation,
        history: List<NotificationHistory> = emptyList()
    ): List<TimelineDayView> {
        val config = IncubationManager.getConfig(incubation)
        // Parse start date, default to today if fail
        val startDate = try { LocalDate.parse(incubation.startDate) } catch (e: Exception) { LocalDate.now() }
        val today = LocalDate.now()
        
        val timeline = mutableListOf<TimelineDayView>()
        
        // Range: Day 1 -> IncubationDays + Buffer (e.g. 3 days post hatch)
        val endDay = config.incubationDays + 3
        
        for (i in 0 until endDay) {
            val dayNum = i + 1
            val date = startDate.plusDays(i.toLong())
            
            // Determine Status
            var status = when {
                date.isBefore(today) -> TimelineStatus.COMPLETED
                date.isEqual(today) -> TimelineStatus.TODAY
                else -> TimelineStatus.UPCOMING
            }
            
            // Calculate Phase
            val phase = when {
                dayNum > config.incubationDays + 2 -> IncubationPhase.OVERDUE
                dayNum >= config.hatchWindowStartDay -> IncubationPhase.HATCH_WINDOW
                dayNum >= config.lockdownDay -> IncubationPhase.LOCKDOWN
                dayNum > (config.incubationDays / 2) -> IncubationPhase.MID_INCUBATION
                else -> IncubationPhase.EARLY_INCUBATION
            }
            
            // Generate Events
            val events = mutableListOf<TimelineEvent>()
            
            // 1. Milestones
            if (dayNum == 1) events.add(TimelineEvent("Incubation Started", "Day 1 of ${config.incubationDays}", isCompleted = true))
            if (dayNum == config.lockdownDay) {
                events.add(TimelineEvent("Lockdown Begins", "Stop turning, increase humidity.", isCritical = true))
                if (status != TimelineStatus.COMPLETED) status = TimelineStatus.CRITICAL // Lockdown is critical
            }
            if (dayNum == config.hatchWindowStartDay) {
                events.add(TimelineEvent("Hatch Window", "Chicks may pip.", isCritical = true))
            }
            if (dayNum == config.incubationDays) {
                events.add(TimelineEvent("Expected Hatch Day", "Main hatch day.", isCritical = true))
            }
            
            // 2. Scheduled Tasks
            if (dayNum <= config.turningUntilDay) {
                events.add(TimelineEvent("Turn Eggs", "3-5 times daily"))
            }
            if (dayNum == config.turningUntilDay + 1) {
                events.add(TimelineEvent("Stop Turning", "Final turn today.", isCritical = true))
            }
            
            // Cooling
            val coolingStartDay = config.coolingStartDay
            if (coolingStartDay != null && dayNum >= coolingStartDay && dayNum < config.lockdownDay) {
                events.add(TimelineEvent("Cool Eggs", "${config.coolingDurationMinutes} mins"))
            }
            
            val finalizedEvents = if (date.isBefore(today)) {
                events.map { it.copy(isCompleted = true) }
            } else {
                events
            }
            
            timeline.add(TimelineDayView(
                dayNum, 
                date, 
                status, 
                finalizedEvents, 
                phase
            ))
        }
        
        return timeline
    }
}

