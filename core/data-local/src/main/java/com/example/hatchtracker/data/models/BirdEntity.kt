package com.example.hatchtracker.data.models

import androidx.room.*
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.model.SyncState
import com.example.hatchtracker.model.BirdLifecycleStage
import com.example.hatchtracker.model.genetics.BreedContribution
import com.example.hatchtracker.data.models.Syncable

@Entity(
    tableName = "birds",
    indices = [
        Index(value = ["flockId"]),
        Index(value = ["motherId"]),
        Index(value = ["fatherId"]),
        Index(value = ["incubationId"])
    ]
)
data class BirdEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val flockId: Long? = null,
    val species: String,
    @ColumnInfo(defaultValue = "'undefined'")
    val breed: String,
    val breedId: String = "",
    val sex: Sex = Sex.UNKNOWN,
    val generation: Int = 0,
    val generationLabel: String? = null, // F1, F2, BC1, etc. (Derived-first; persisted as override)
    @ColumnInfo(defaultValue = "''")
    val hatchDate: String = "",
    val motherId: Long? = null,
    val fatherId: Long? = null,
    
    // Genetics Phase 2: Breed Composition (Stable ID + Percentage)
    @ColumnInfo(defaultValue = "'[]'")
    val breedComposition: List<BreedContribution> = emptyList(),
    
    val incubationId: Long? = null,
    val hatchBatchId: Long? = null,
    val color: String? = null,
    val notes: String? = null,
    override val syncId: String = java.util.UUID.randomUUID().toString(),
    val status: String = "active",
    @ColumnInfo(defaultValue = "'ADULT'")
    val lifecycleStage: BirdLifecycleStage = BirdLifecycleStage.ADULT,
    val lastUpdated: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    
    // Genetics B2: Hybrid persistence
    @Embedded(prefix = "genetic_")
    val geneticProfile: GeneticProfile = GeneticProfile(),
    val geneticProfileJson: String? = null,
    
    @Embedded(prefix = "custom_")
    val customGeneticProfile: GeneticProfile? = null,
    val customGeneticProfileJson: String? = null,

    // High-value summary for filtering
    val geneticsSummary_speciesCode: String? = null,
    val geneticsSummary_sexLinkedFlags: Int? = null,
    val geneticsSummary_hasLethalCarrier: Boolean? = null,

    val ringNumber: String? = null,
    
    // Sync Fields
    val ownerUserId: String? = null,
    
    // Multi-user Support (Phase 5)
    @ColumnInfo(defaultValue = "'USER'")
    val scopeType: String = "USER",
    val scopeId: String? = null,

    @ColumnInfo(defaultValue = "''")
    val cloudId: String = syncId,
    val flockCloudId: String? = null,
    val motherCloudId: String? = null,
    val fatherCloudId: String? = null,
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

    // Lifecycle Costing (Option B)
    @ColumnInfo(defaultValue = "0")
    val costBasisCents: Long = 0,
    val costBasisSourceRef: String? = null
) : Syncable {
    override val lastModified: Long get() = localUpdatedAt
    override val cloudUpdatedAt: Long? get() = serverUpdatedAt
}
