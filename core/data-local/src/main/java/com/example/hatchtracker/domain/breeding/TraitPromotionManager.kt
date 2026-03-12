package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BirdEntity
import com.example.hatchtracker.data.BirdEntityDao
import com.example.hatchtracker.data.TraitPromotionDao
import com.example.hatchtracker.data.models.TraitPromotion
import com.example.hatchtracker.data.models.ConfidenceLevel
import androidx.room.withTransaction
import javax.inject.Inject

class TraitPromotionManager @Inject constructor(
    private val database: com.example.hatchtracker.data.AppDatabase, // Inject Database for transactions
    private val birdDao: BirdEntityDao,
    private val promotionDao: TraitPromotionDao
) {

    /**
     * Promotes an inferred trait to a fixed trait for a bird.
     * Records an audit log for the action.
     */
    suspend fun promoteTrait(
        bird: BirdEntity,
        traitName: String,
        adminId: String,
        reason: String? = null
    ): Result<Unit> {
        return try {
            database.withTransaction {
                val profile = bird.geneticProfile
                
                // Check if trait is actually inferred
                if (!profile.inferredTraits.contains(traitName)) {
                    throw IllegalArgumentException("Trait '$traitName' is not in inferred list.")
                }

                // Move trait
                val newInferred = profile.inferredTraits.filter { it != traitName }
                val newFixed = (profile.fixedTraits + traitName).distinct()
                
                val updatedProfile = profile.copy(
                    inferredTraits = newInferred,
                    fixedTraits = newFixed,
                    // Optionally increase confidence if it was lower
                    confidenceLevel = if (profile.confidenceLevelEnum == ConfidenceLevel.LOW) {
                        ConfidenceLevel.MEDIUM.name
                    } else {
                        profile.confidenceLevel
                    }
                )

                val updatedBird = bird.copy(
                    geneticProfile = updatedProfile,
                    lastUpdated = System.currentTimeMillis()
                )

                // Persist changes
                birdDao.updateBirdEntity(updatedBird)
                
                // Record promotion
                promotionDao.insertPromotion(TraitPromotion(
                    birdId = bird.id,
                    traitName = traitName,
                    oldStatus = "INFERRED",
                    newStatus = "FIXED",
                    promotedBy = adminId,
                    reason = reason
                ))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Boosts the confidence level of a bird's genetic profile based on community data.
     */
    suspend fun boostConfidence(
        bird: BirdEntity,
        newLevel: ConfidenceLevel,
        adminId: String,
        reason: String? = null
    ): Result<Unit> {
        return try {
            database.withTransaction {
                val updatedBird = bird.copy(
                    geneticProfile = bird.geneticProfile.copy(confidenceLevel = newLevel.name),
                    lastUpdated = System.currentTimeMillis()
                )
                
                birdDao.updateBirdEntity(updatedBird)
                
                promotionDao.insertPromotion(TraitPromotion(
                    birdId = bird.id,
                    traitName = "CONFIDENCE_BOOST",
                    oldStatus = bird.geneticProfile.confidenceLevel,
                    newStatus = newLevel.name,
                    promotedBy = adminId,
                    reason = reason ?: "Confidence boosted by admin based on community data."
                ))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
