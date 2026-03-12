package com.example.hatchtracker.feature.incubation.models

import com.example.hatchtracker.core.domain.models.StandardRowModel
import com.example.hatchtracker.data.models.Incubation

/**
 * Lightweight DTO for rendering Incubation lists without heavy UI state aggregation. 
 */
@androidx.compose.runtime.Immutable
data class IncubationRowModel(
    override val id: String,
    val title: String,
    val subtitle: String,
    val statusText: String,
    override val urgencyScore: Int,
    override val isCompletedOrArchived: Boolean,
    override val dueToday: Boolean,
    override val dueWithin7Days: Boolean,
    
    // Feature specific data kept minimal:
    val deviceName: String?,
    val eggsCount: Int,
    val isCompleted: Boolean,
    val originalIncubation: Incubation
) : StandardRowModel
