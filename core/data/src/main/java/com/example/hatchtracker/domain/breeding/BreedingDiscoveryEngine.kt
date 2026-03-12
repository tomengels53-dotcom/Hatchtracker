package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.model.Species
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recommendation for a breeding pair.
 */
data class BreedingSuggestion(
    val maleId: Long,
    val femaleId: Long,
    val score: Double,
    val predictedProbability: Double,
    val confidence: ConfidenceLevel,
    val explanation: String
)

/**
 * Engine to identify optimal breeding pairs using the canonical BreedingPredictionService.
 */
@Singleton
class BreedingDiscoveryEngine @Inject constructor(
    private val breedingPredictionService: BreedingPredictionService
) {

    /**
     * Suggests breeding pairs sorted by the probability of achieving a target trait.
     */
    fun suggestBreedingPairs(
        birds: List<Bird>,
        targetTrait: String,
        maxResults: Int = 5
    ): List<BreedingSuggestion> {
        val males = birds.filter { it.sex == com.example.hatchtracker.data.models.Sex.MALE }
        val females = birds.filter { it.sex == com.example.hatchtracker.data.models.Sex.FEMALE }

        val suggestions = mutableListOf<BreedingSuggestion>()

        for (male in males) {
            for (female in females) {
                // 1. Inbreeding Prevention
                if (isRelated(male, female)) continue

                // 2. Predict Traits via Canonical Service
                val species = try { 
                    Species.valueOf(male.species.name) 
                } catch (e: Exception) { 
                    Species.CHICKEN 
                }

                val result = breedingPredictionService.predictBreeding(
                    species = species,
                    sireProfile = male.geneticProfile,
                    damProfile = female.geneticProfile
                )

                val targetPrediction = result.phenotypeResult.probabilities
                    .firstOrNull { it.phenotypeId == targetTrait }

                if (targetPrediction != null) {
                    val score = targetPrediction.probability * 0.9 // High confidence weight
                    suggestions.add(
                        BreedingSuggestion(
                            maleId = male.localId,
                            femaleId = female.localId,
                            score = score,
                            predictedProbability = targetPrediction.probability,
                            confidence = ConfidenceLevel.HIGH,
                            explanation = "Calculated via canonical prediction pipeline."
                        )
                    )
                }
            }
        }

        return suggestions
            .sortedByDescending { it.score }
            .take(maxResults)
    }

    private fun isRelated(birdA: Bird, birdB: Bird): Boolean {
        if (birdA.motherId != null && birdA.motherId == birdB.motherId) return true
        if (birdA.fatherId != null && birdA.fatherId == birdB.fatherId) return true
        return false
    }
}

