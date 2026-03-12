package com.example.hatchtracker.model

import java.util.UUID

/**
 * Standard business model for a Flocklet (juvenile bird batch).
 */
data class Flocklet(
    val id: Long = 0,
    val hatchId: Long? = null,
    val species: String,
    val breeds: List<String> = listOf("Mixed"),
    val hatchDate: Long,
    val chickCount: Int,
    
    val currentTemp: Double = 0.0,
    val targetTemp: Double = 0.0,
    val ageInDays: Int = 0,
    val weightAvg: Double = 0.0,
    val healthStatus: String = "Healthy",
    val notes: String? = null,
    
    val readyForFlock: Boolean = false,
    val movedToFlockId: Long? = null,
    
    val syncId: String = UUID.randomUUID().toString(),
    val lifecycleStage: BirdLifecycleStage = BirdLifecycleStage.FLOCKLET,
    val lastUpdated: Long = System.currentTimeMillis(),

    // Lifecycle Costing
    val costBasisCents: Long = 0,
    val costBasisSourceRef: String? = null,
    val costBasisSchemaVersion: Int = 1
)
