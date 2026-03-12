package com.example.hatchtracker.data.models

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "egg_production",
    indices = [
        Index(value = ["flockId", "dateEpochDay"]),
        Index(value = ["flockId", "lineId", "dateEpochDay"], unique = true),
        Index(value = ["cloudId"], unique = true)
    ]
)
data class EggProductionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val flockId: String,
    val lineId: String? = null,
    
    val dateEpochDay: Long,
    
    val totalEggs: Int,
    val crackedEggs: Int = 0,
    val setForIncubation: Int = 0,
    /** Cached count of eggs allocated to non-cancelled sales. Rebuildable from egg_sale_allocation. */
    @ColumnInfo(defaultValue = "0")
    val soldEggs: Int = 0,
    val notes: String? = null,
    
    // Multi-user Support (Phase 5)
    @ColumnInfo(defaultValue = "'USER'")
    val scopeType: String = "USER",
    val scopeId: String? = null,

    // Sync Metadata
    val cloudId: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING
) {
    val tableEggs: Int
        get() = (totalEggs - crackedEggs - setForIncubation - soldEggs).coerceAtLeast(0)
        
    init {
        require(totalEggs >= 0) { "Total eggs cannot be negative" }
        require(crackedEggs >= 0) { "Cracked eggs cannot be negative" }
        require(setForIncubation >= 0) { "Set eggs cannot be negative" }
        require(soldEggs >= 0) { "Sold eggs cannot be negative" }
    }
}
