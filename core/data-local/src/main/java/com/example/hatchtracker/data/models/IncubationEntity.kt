package com.example.hatchtracker.data.models

import androidx.room.*
import com.example.hatchtracker.model.IncubationLike
import com.example.hatchtracker.model.BirdLifecycleStage
import com.example.hatchtracker.model.SyncState

@Entity(
    tableName = "incubations",
    indices = [
        Index(value = ["flockId"]),
        Index(value = ["birdId"]),
        Index(value = ["fatherBirdId"])
    ]
)
data class IncubationEntity(
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    val flockId: Long? = null,
    val birdId: Long? = null,
    val fatherBirdId: Long? = null,
    override val species: String,
    @ColumnInfo(defaultValue = "'[]'")
    val breeds: List<String> = emptyList(),
    override val startDate: String,
    val expectedHatch: String,
    val eggsCount: Int,
    val hatchedCount: Int = 0,
    val infertileCount: Int = 0,
    val failedCount: Int = 0,
    val actualHatchDate: String? = null,
    val hatchNotes: String? = null,
    val notes: String? = null,
    override val hatchCompleted: Boolean = false,
    val sourceScenarioId: String = "",
    val incubationProfileId: String = "",
    val incubatorDeviceId: String = "",
    val hatcherDeviceId: String = "",
    override val syncId: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "'INCUBATING'")
    override val lifecycleStage: BirdLifecycleStage = BirdLifecycleStage.INCUBATING,
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Sync Fields
    val ownerUserId: String? = null,
    
    // Multi-user Support (Phase 5)
    @ColumnInfo(defaultValue = "'USER'")
    val scopeType: String = "USER",
    val scopeId: String? = null,

    @ColumnInfo(defaultValue = "''")
    val cloudId: String = syncId,
    val flockCloudId: String? = null,
    val birdCloudId: String? = null,
    val fatherBirdCloudId: String? = null,
    val serverUpdatedAt: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val localUpdatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    override val deleted: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val pendingSync: Boolean = true,
    @ColumnInfo(defaultValue = "'PENDING'")
    override val syncState: SyncState = if (pendingSync) SyncState.PENDING else SyncState.SYNCED,
    override val syncError: String? = null,
    val actionPlanId: String? = null,
    val generationIndex: Int? = null,
    val breedLabelOverride: String? = null,
    @ColumnInfo(defaultValue = "'[]'")
    val breederPoolBirdIds: List<String> = emptyList(),

    // Lifecycle Costing (Option B)
    @ColumnInfo(defaultValue = "0")
    val costBasisCents: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val assetAllocationCents: Long = 0,
    @ColumnInfo(defaultValue = "1")
    val costBasisSchemaVersion: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val isCostFrozen: Boolean = false
) : IncubationLike, Syncable {
    override val lastModified: Long get() = localUpdatedAt
    override val cloudUpdatedAt: Long? get() = serverUpdatedAt
}
