package com.example.hatchtracker.data.remote.models

/**
 * Firestore document representation of a maintenance log.
 */
data class MaintenanceLogDocument(
    val id: String = "",
    val equipmentId: String = "",
    val date: Long = 0,
    val type: String = "",
    val description: String = "",
    val cost: Double? = null
)
