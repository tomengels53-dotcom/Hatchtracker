package com.example.hatchtracker.core.common

import java.time.LocalDate

// Note: IncubationPhase enum is defined in IncubationManager.kt

/**
 * Specific action required for a given day.
 */
data class TimelineAction(
    val title: String,
    val description: String,
    val isCritical: Boolean = false,
    val notificationRuleId: String? = null
)

/**
 * Representation of a single day in the incubation cycle.
 */
data class TimelineDay(
    val dayNumber: Int,
    val date: LocalDate,
    val phase: IncubationPhase,
    val tempTarget: Double,
    val humidityRange: Pair<Int, Int>,
    val actions: List<TimelineAction> = emptyList(),
    val isPast: Boolean = false
)
