package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hatchtracker.core.domain.models.LedgerEntityType
import com.example.hatchtracker.core.domain.models.LedgerSourceType
import java.util.UUID

@Entity(tableName = "cost_basis_ledger")
data class CostBasisLedgerEntryEntity(
    @PrimaryKey
    val entryId: String = UUID.randomUUID().toString(),
    val entityType: LedgerEntityType = LedgerEntityType.INCUBATION,
    val entityId: String = "", // Sync ID or String-ified ID
    val sourceType: LedgerSourceType = LedgerSourceType.DIRECT_COST,
    val amount: Double = 0.0, // positive = cost addition, negative = inventory removal ONLY
    val ownerUserId: String = "",
    val syncStateInt: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Core Data Sync fields
    override val syncId: String = entryId,
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
    val cloudId: String = entryId
) : Syncable {
    val id: Long get() = entryId.hashCode().toLong()
    override val lastModified: Long get() = localUpdatedAt
    override val cloudUpdatedAt: Long? get() = serverUpdatedAt
}
