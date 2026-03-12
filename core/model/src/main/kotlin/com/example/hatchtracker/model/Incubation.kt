package com.example.hatchtracker.model

import java.util.UUID

/**
 * Canonical Incubation domain model.
 */
data class Incubation(
    override val id: Long = 0,
    val flockId: Long? = null,
    val birdId: Long? = null,
    val fatherBirdId: Long? = null,
    override val species: String = "",
    val breeds: List<String> = emptyList(),
    override val startDate: String = "",
    val expectedHatch: String = "",
    val eggsCount: Int = 0,
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
    val syncId: String = UUID.randomUUID().toString(),
    override val lifecycleStage: BirdLifecycleStage = BirdLifecycleStage.INCUBATING,
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Sync Metadata
    val ownerUserId: String? = null,
    val cloudId: String = syncId,
    val flockCloudId: String? = null,
    val birdCloudId: String? = null,
    val fatherBirdCloudId: String? = null,
    val serverUpdatedAt: Long? = null,
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING,
    
    val actionPlanId: String? = null,
    val generationIndex: Int? = null,
    val breedLabelOverride: String? = null,
    val breederPoolBirdIds: List<String> = emptyList(),

    // Lifecycle Costing
    val costBasisCents: Long = 0,
    val assetAllocationCents: Long = 0,
    val costBasisSchemaVersion: Int = 1,
    val isCostFrozen: Boolean = false
) : IncubationLike
