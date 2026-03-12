package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hatchtracker.core.domain.models.AssetScopeType

@Entity(tableName = "asset_allocation_events")
data class AssetAllocationEventEntity(
    @PrimaryKey
    val allocationId: String = "", // Deterministic: assetId_scopeType_scopeId_periodKey
    val assetId: String = "",
    val scopeType: AssetScopeType = AssetScopeType.INCUBATION,
    val scopeId: String = "", // using string to map easily to syncId or direct long if encoded
    val periodKey: String = "", // e.g. yyyy-MM-dd for daily, yyyy-MM for monthly, or "cycle_x"
    val amount: Double = 0.0,
    val ownerUserId: String = "",
    val syncStateInt: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Core Data Sync fields
    override val syncId: String = allocationId,
    override val deleted: Boolean = (syncStateInt == -1),
    val pendingSync: Boolean = (syncStateInt == 1),
    override val syncState: SyncState = when (syncStateInt) {
        -1 -> SyncState.PENDING
        1 -> SyncState.PENDING
        else -> SyncState.SYNCED
    },
    override val syncError: String? = null,
    val localUpdatedAt: Long = createdAt,
    val serverUpdatedAt: Long? = null,
    val cloudId: String = allocationId
) : Syncable {
    val id: Long get() = allocationId.hashCode().toLong()
    override val lastModified: Long get() = localUpdatedAt
    override val cloudUpdatedAt: Long? get() = serverUpdatedAt
}
