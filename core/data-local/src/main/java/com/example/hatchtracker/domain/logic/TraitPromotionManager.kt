package com.example.hatchtracker.domain.logic

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.BirdEntity
import com.example.hatchtracker.data.models.ConfidenceLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backward-compatible shim for legacy references to domain.logic.TraitPromotionManager.
 * Delegates to the canonical com.example.hatchtracker.domain.breeding.TraitPromotionManager.
 */
@Singleton
class TraitPromotionManager @Inject constructor(
    private val delegate: com.example.hatchtracker.domain.breeding.TraitPromotionManager
) {
    suspend fun promoteTrait(
        bird: Bird,
        traitName: String,
        adminId: String,
        reason: String? = null
    ): Result<Unit> = delegate.promoteTrait(bird.toEntity(), traitName, adminId, reason)

    suspend fun boostConfidence(
        bird: Bird,
        newLevel: ConfidenceLevel,
        adminId: String,
        reason: String? = null
    ): Result<Unit> = delegate.boostConfidence(bird.toEntity(), newLevel, adminId, reason)

    private fun Bird.toEntity(): BirdEntity {
        return BirdEntity(
            id = localId,
            flockId = flockId,
            species = species.name,
            breed = breed,
            breedId = breedId,
            sex = sex,
            hatchDate = hatchDate,
            generation = generation,
            motherId = motherId,
            fatherId = fatherId,
            incubationId = incubationId,
            hatchBatchId = hatchBatchId,
            color = color,
            notes = notes,
            syncId = syncId,
            status = status,
            lifecycleStage = lifecycleStage,
            lastUpdated = lastUpdated,
            imagePath = imagePath,
            geneticProfile = geneticProfile,
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
    }
}
