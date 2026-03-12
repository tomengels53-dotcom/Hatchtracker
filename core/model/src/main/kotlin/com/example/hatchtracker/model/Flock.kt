package com.example.hatchtracker.model

import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.model.SyncState

/**
 * Canonical Flock domain model.
 */
data class Flock(
    val localId: Long = 0,
    val syncId: String = "",
    val species: Species = Species.UNKNOWN,
    val breeds: List<String> = emptyList(),
    val name: String = "",
    val purpose: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val eggCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val defaultGeneticProfile: GeneticProfile? = null,
    val ownerUserId: String? = null,
    
    // Sync Metadata
    val cloudId: String = syncId,
    val serverUpdatedAt: Long? = null,
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING
) {
    val id: Long get() = localId
}
