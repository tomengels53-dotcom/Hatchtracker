package com.example.hatchtracker.feature.flock.models

import com.example.hatchtracker.core.domain.models.StandardRowModel
import com.example.hatchtracker.data.models.Flock

/**
 * Lightweight DTO for rendering Flock lists without heavy UI state aggregation. 
 */
@androidx.compose.runtime.Immutable
data class FlockRowModel(
    override val id: String,
    val title: String,
    val subtitle: String,
    val statusText: String?,
    override val urgencyScore: Int,
    override val isCompletedOrArchived: Boolean,
    override val dueToday: Boolean, // Rarely true for permanent flocks unless some specific alert triggers
    override val dueWithin7Days: Boolean,
    
    // Feature specific data kept minimal:
    val birdCount: Int,
    val isArchived: Boolean,
    val originalFlock: Flock
) : StandardRowModel
