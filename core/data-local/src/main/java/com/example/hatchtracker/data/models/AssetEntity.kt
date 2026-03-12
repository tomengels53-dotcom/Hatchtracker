package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hatchtracker.core.domain.models.AssetCategory
import com.example.hatchtracker.core.domain.models.AssetStatus
import com.example.hatchtracker.core.domain.models.DepreciationMethod
import java.util.UUID

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey
    val assetId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: AssetCategory = AssetCategory.OTHER,
    val linkedDeviceId: String? = null,
    val purchaseDateEpochMs: Long = System.currentTimeMillis(),
    val purchasePrice: Double = 0.0,
    val residualValue: Double = 0.0,
    val depreciationMethod: DepreciationMethod = DepreciationMethod.TIME_BASED,
    val usefulLifeMonths: Int? = null,
    val expectedCycles: Int? = null,
    val cyclesAllocatedCount: Int = 0,
    val lastAllocatedAtEpochMs: Long? = null,
    val retiredDateEpochMs: Long? = null,
    val retirementValue: Double? = null,
    val status: AssetStatus = AssetStatus.ACTIVE,
    val ownerUserId: String = "",
    val syncStateInt: Int = 0, // 0 = synced, 1 = pending, -1 = deleted
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Core Data Sync fields mapping to required interfaces if any
    override val syncId: String = assetId,
    override val deleted: Boolean = (syncStateInt == -1),
    val pendingSync: Boolean = (syncStateInt == 1),
    override val syncState: SyncState = when (syncStateInt) {
        -1 -> SyncState.PENDING
        1 -> SyncState.PENDING
        else -> SyncState.SYNCED
    },
    override val syncError: String? = null,
    val localUpdatedAt: Long = updatedAt,
    val serverUpdatedAt: Long? = null,
    val cloudId: String = assetId
) : Syncable {
    val id: Long get() = assetId.hashCode().toLong()
    override val lastModified: Long get() = localUpdatedAt
    override val cloudUpdatedAt: Long? get() = serverUpdatedAt

    val depreciableBase: Double
        get() = (purchasePrice - residualValue).coerceAtLeast(0.0)
}
