package com.example.hatchtracker.model

import java.util.UUID

/**
 * Represents a maintenance or service event for a piece of equipment.
 */
data class EquipmentMaintenanceLog(
    val id: String = UUID.randomUUID().toString(),
    val equipmentId: String,
    val date: Long = System.currentTimeMillis(),
    val type: MaintenanceLogType = MaintenanceLogType.OTHER,
    val description: String = "",
    val cost: Double? = null
)

enum class MaintenanceLogType {
    CLEANING,
    REPAIR,
    INSPECTION,
    CALIBRATION,
    UPGRADE,
    OTHER
}
