package com.example.hatchtracker.model

import java.util.UUID

/**
 * Standard business model for an Egg Production entry.
 */
data class EggProduction(
    val id: String = UUID.randomUUID().toString(),
    val flockId: String,
    val lineId: String? = null,
    val dateEpochDay: Long,
    val totalEggs: Int,
    val crackedEggs: Int = 0,
    val setForIncubation: Int = 0,
    val soldEggs: Int = 0,
    val notes: String? = null,
    
    // Sync Metadata
    val cloudId: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING
) {
    val tableEggs: Int
        get() = (totalEggs - crackedEggs - setForIncubation - soldEggs).coerceAtLeast(0)
}
