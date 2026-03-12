package com.example.hatchtracker.feature.nursery.models

import com.example.hatchtracker.core.domain.models.StandardRowModel
import com.example.hatchtracker.data.models.Flocklet

/**
 * Lightweight DTO for rendering Nursery lists without heavy UI state aggregation. 
 */
@androidx.compose.runtime.Immutable
data class NurseryRowModel(
    override val id: String,
    val title: String,
    val subtitle: String,
    val statusText: String,
    override val urgencyScore: Int,
    override val isCompletedOrArchived: Boolean,
    override val dueToday: Boolean,
    override val dueWithin7Days: Boolean,
    
    // Feature specific data kept minimal:
    val speciesName: String,
    val chickCount: Int,
    val originalFlocklet: Flocklet 
) : StandardRowModel
