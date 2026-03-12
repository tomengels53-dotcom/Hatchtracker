package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.model.BreedStandard
import com.example.hatchtracker.model.BreedingGoal
import com.example.hatchtracker.model.TraitLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a breeding suggestion calculation.
 */
data class BreedingPairSuggestion(
    val hen: Bird,
    val rooster: Bird,
    val score: Int, // 0 - 100
    val explanation: String,
    val predictedTraits: List<String>
)

/**
 * Service to calculate and suggest optimal breeding pairs based on genetic data and user goals.
 * Uses repository truth and typed enums from the Trait Ontology.
 */
@Singleton
class BreedingAdvisor @Inject constructor(
    private val breedRepository: BreedStandardRepository
) {

    /**
     * Suggests breeding pairs from a list of birds based on a breeding goal and known lineage.
     */
    fun suggestPairs(
        hens: List<Bird>,
        roosters: List<Bird>,
        allBirds: List<Bird>,
        goal: BreedingGoal
    ): List<BreedingPairSuggestion> {
        val suggestions = mutableListOf<BreedingPairSuggestion>()

        for (hen in hens) {
            for (rooster in roosters) {
                if (hen.species != rooster.species) continue

                val result = analyzePair(hen, rooster, allBirds, goal)
                if (result.score >= 0) {
                    suggestions.add(result)
                }
            }
        }

        return suggestions.sortedByDescending { it.score }
    }

    private fun analyzePair(
        hen: Bird,
        rooster: Bird,
        allBirds: List<Bird>,
        goal: BreedingGoal
    ): BreedingPairSuggestion {
        var score = 70 // Base score
        val explanations = mutableListOf<String>()
        val predictions = mutableListOf<String>()

        val henStandard = breedRepository.getBreedById(hen.breedId.ifBlank { hen.breed.lowercase() })
        val roosterStandard = breedRepository.getBreedById(rooster.breedId.ifBlank { rooster.breed.lowercase() })

        // 1. Inbreeding Check
        val relationshipPenalty = calculateRelationshipPenalty(hen, rooster, goal.inbreedingTolerance)
        if (relationshipPenalty > 0) {
            score -= relationshipPenalty
            explanations.add("Lineage penalty: Close genetic relationship detected (-$relationshipPenalty).")
        } else {
            explanations.add("Genetic diversity: No immediate common ancestors found.")
        }

        // 2. Goal Alignment: Egg Color
        goal.targetEggColor?.let { target ->
            val henColor = henStandard?.normalizedEggColor
            if (henColor == target) {
                score += 15
                explanations.add("Goal match: Hen's breed known for $target eggs.")
            } else if (henStandard?.eggColor?.contains(target.name, ignoreCase = true) == true) {
                // Heuristic fallback if normalized field is missing but string matches
                score += 10
                explanations.add("Partial match: Hen's breed info indicates $target eggs.")
            }
        }

        // 3. Goal Alignment: Climate / Hardiness
        goal.climate?.let { targetHardiness ->
            val henHardiness = henStandard?.coldHardiness
            val roosterHardiness = roosterStandard?.coldHardiness
            
            if (henHardiness == TraitLevel.HIGH) {
                score += 10
                explanations.add("Environment: Female is cold hardy.")
            }
            if (roosterHardiness == TraitLevel.HIGH) {
                score += 10
                explanations.add("Environment: Male is cold hardy.")
            }
        }

        // 4. Goal Alignment: Size
        goal.sizePreference?.let { targetSize ->
            if (henStandard?.normalizedBodySize == targetSize) {
                score += 5
                explanations.add("Size match: Female fits $targetSize preference.")
            }
        }

        // 5. Trait Predictions (Simplified Probability)
        if (henStandard != null && roosterStandard != null) {
            if (henStandard.normalizedEggColor == roosterStandard.normalizedEggColor && henStandard.normalizedEggColor != null) {
                predictions.add("High probability of ${henStandard.normalizedEggColor} eggs.")
            } else {
                predictions.add("Hybrid offspring likely; mixed traits predicted.")
            }
        }

        // Ensure score bounds
        score = score.coerceIn(0, 100)

        return BreedingPairSuggestion(
            hen = hen,
            rooster = rooster,
            score = score,
            explanation = explanations.joinToString(" "),
            predictedTraits = predictions
        )
    }

    private fun calculateRelationshipPenalty(
        hen: Bird,
        rooster: Bird,
        tolerance: String
    ): Int {
        val motherMatch = hen.motherId != null && hen.motherId != 0L && hen.motherId == rooster.motherId
        val fatherMatch = hen.fatherId != null && hen.fatherId != 0L && hen.fatherId == rooster.fatherId
        
        if (motherMatch || fatherMatch) {
             return if (tolerance == "high") 30 else 60
        }

        if ((hen.motherId == rooster.localId) || (hen.fatherId == rooster.localId)) return 80
        if ((rooster.motherId == hen.localId) || (rooster.fatherId == hen.localId)) return 80

        return 0
    }
}
