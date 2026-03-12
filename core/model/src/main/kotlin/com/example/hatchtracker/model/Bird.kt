package com.example.hatchtracker.model

import com.example.hatchtracker.model.genetics.BreedContribution
import java.util.UUID

/**
 * Canonical Bird domain model.
 * decoupled from Room and Firestore schemas.
 */
data class Bird(
    val localId: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val flockId: Long? = null,
    override val species: Species = Species.UNKNOWN,
    val breed: String = "",
    val breedId: String = "",
    override val sex: Sex = Sex.UNKNOWN,
    val hatchDate: String = "",
    val generation: Int = 0,
    val generationLabel: String? = null, // F1, F2, BC1, etc. (Stored as override)
    val motherId: Long? = null,
    val fatherId: Long? = null,
    
    // Genetics Phase 2: Breed Composition (Stable ID + Percentage)
    val breedComposition: List<BreedContribution> = emptyList(),
    
    val incubationId: Long? = null,
    val hatchBatchId: Long? = null,
    val color: String? = null,
    val notes: String? = null,
    val status: String = "active",
    val lifecycleStage: BirdLifecycleStage = BirdLifecycleStage.ADULT,
    val lastUpdated: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val customGeneticProfile: GeneticProfile? = null,
    override val geneticProfile: GeneticProfile = customGeneticProfile ?: GeneticProfile(),
    val ringNumber: String? = null,
    val ownerUserId: String? = null,
    
    // Sync Metadata (Internal to model, but exposed for repository orchestration)
    val cloudId: String = syncId,
    val serverUpdatedAt: Long? = null,
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING,
    
    // Lifecycle Costing
    val costBasisCents: Long = 0,
    val costBasisSourceRef: String? = null
) : GeneticSource {
    constructor(
        localId: Long = 0,
        syncId: String = UUID.randomUUID().toString(),
        flockId: Long? = null,
        species: String,
        breed: String,
        breedId: String = "",
        sex: Sex = Sex.UNKNOWN,
        hatchDate: String,
        generation: Int = 0,
        generationLabel: String? = null,
        motherId: Long? = null,
        fatherId: Long? = null,
        breedComposition: List<BreedContribution> = emptyList(),
        incubationId: Long? = null,
        hatchBatchId: Long? = null,
        color: String? = null,
        notes: String? = null,
        status: String = "active",
        lifecycleStage: BirdLifecycleStage = BirdLifecycleStage.ADULT,
        lastUpdated: Long = System.currentTimeMillis(),
        imagePath: String? = null,
        customGeneticProfile: GeneticProfile? = null,
        ringNumber: String? = null,
        ownerUserId: String? = null,
        cloudId: String = syncId,
        serverUpdatedAt: Long? = null,
        localUpdatedAt: Long = System.currentTimeMillis(),
        deleted: Boolean = false,
        syncState: SyncState = SyncState.PENDING,
        costBasisCents: Long = 0,
        costBasisSourceRef: String? = null
    ) : this(
        localId = localId,
        syncId = syncId,
        flockId = flockId,
        species = parseSpecies(species),
        breed = breed,
        breedId = breedId,
        sex = sex,
        hatchDate = hatchDate,
        generation = generation,
        generationLabel = generationLabel,
        motherId = motherId,
        fatherId = fatherId,
        breedComposition = breedComposition,
        incubationId = incubationId,
        hatchBatchId = hatchBatchId,
        color = color,
        notes = notes,
        status = status,
        lifecycleStage = lifecycleStage,
        lastUpdated = lastUpdated,
        imagePath = imagePath,
        customGeneticProfile = customGeneticProfile,
        geneticProfile = customGeneticProfile ?: GeneticProfile(),
        ringNumber = ringNumber,
        ownerUserId = ownerUserId,
        cloudId = cloudId,
        serverUpdatedAt = serverUpdatedAt,
        localUpdatedAt = localUpdatedAt,
        deleted = deleted,
        syncState = syncState,
        costBasisCents = costBasisCents,
        costBasisSourceRef = costBasisSourceRef
    )

    val id: Long get() = localId
    override val geneticSourceId: String get() = syncId
    override val displayName: String get() = "$breed ($localId)"

    companion object {
        private fun parseSpecies(raw: String): Species {
            return try {
                Species.valueOf(raw.trim().uppercase())
            } catch (_: IllegalArgumentException) {
                Species.UNKNOWN
            }
        }
    }
}
