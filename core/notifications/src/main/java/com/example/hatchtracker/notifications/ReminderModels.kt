package com.example.hatchtracker.notifications

import com.example.hatchtracker.model.BirdLifecycleStage

enum class ReminderCategory {
    INCUBATION,
    NURSERY,
    FLOCK
}

data class HatchyReminder(
    val id: String,
    val category: ReminderCategory,
    val title: String,
    val message: String,
    val isCritical: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Rules for when a reminder should trigger.
 */
sealed class ReminderRule {
    data class IncubationLockdown(val incubationId: Long) : ReminderRule()
    data class IncubationExpectedHatch(val incubationId: Long) : ReminderRule()
    data class NurseryBrooderTemp(val flockletId: Long) : ReminderRule()
    data class FlockReadiness(val flockletId: Long) : ReminderRule()
}

