package com.example.hatchtracker.data.models

import androidx.room.*
import java.util.UUID
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.model.SyncState

@Entity(tableName = "flocks")
data class FlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val species: String, // Immutable after creation
    @ColumnInfo(defaultValue = "[]")
    val breeds: List<String> = emptyList(),
    val name: String,
    val purpose: String, // eggs, breeding, meat, exhibition, mixed
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    
    @ColumnInfo(defaultValue = "0")
    val eggCount: Int = 0,
    
    override val syncId: String = UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    
    // Genetics B2: Hybrid persistence
    @Embedded(prefix = "default_")
    val defaultGeneticProfile: GeneticProfile? = null,
    val defaultGeneticProfileJson: String? = null,

    // Sync Fields
    val ownerUserId: String? = null,
    
    // Multi-user Support (Phase 5)
    @ColumnInfo(defaultValue = "'USER'")
    val scopeType: String = "USER",
    val scopeId: String? = null,

    @ColumnInfo(defaultValue = "''")
    val cloudId: String = syncId,
    val serverUpdatedAt: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val localUpdatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    override val deleted: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val pendingSync: Boolean = true,
    @ColumnInfo(defaultValue = "'PENDING'")
    override val syncState: SyncState = if (pendingSync) SyncState.PENDING else SyncState.SYNCED,
    override val syncError: String? = null
) : Syncable {
    override val lastModified: Long get() = localUpdatedAt
    override val cloudUpdatedAt: Long? get() = serverUpdatedAt
}
