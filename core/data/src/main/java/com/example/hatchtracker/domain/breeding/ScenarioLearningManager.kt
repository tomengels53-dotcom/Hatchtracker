package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingScenario
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.model.GeneticProfile

data class LearningOutcome(
    val updatedScenario: BreedingScenario,
    val revisionNotes: String,
    val promotedTraits: List<String> = emptyList(),
    val confidenceDelta: Float
)

object ScenarioLearningManager {

    /**
     * Refines a scenario based on new evidence (e.g. hatch results or community votes).
     */
    fun processEvidence(
        scenario: BreedingScenario,
        evidenceType: EvidenceType,
        weight: Float // 0.1 to 1.0 based on reliability
    ): LearningOutcome {
        val (newConfidence, delta) = calculateNewConfidence(scenario, evidenceType, weight)
        
        // Logic for trait promotion (Inferred -> Fixed)
        val currentInferred = scenario.generations.flatMap { it.predictedOutcomes }.toMutableList()
        val toPromote = if (newConfidence >= 0.85f) {
            currentInferred.take(1) // Promotion threshold tightened for Scenarios
        } else emptyList()

        val hatchyNote = generateHatchyRevisionNote(evidenceType, delta, toPromote)

        return LearningOutcome(
            updatedScenario = scenario.copy(
                confidenceScore = newConfidence,
                geneticStabilityRatio = (scenario.geneticStabilityRatio + (if (newConfidence > scenario.confidenceScore) 0.1f else -0.1f)).coerceIn(0.0f, 1.0f),
                lastModified = System.currentTimeMillis()
            ),
            revisionNotes = hatchyNote,
            promotedTraits = toPromote,
            confidenceDelta = delta
        )
    }

    private fun calculateNewConfidence(
        scenario: BreedingScenario,
        type: EvidenceType,
        weight: Float // Factor for reliability (e.g. 1.0 for manual verification, 0.5 for automated prediction)
    ): Pair<Float, Float> {
        val current = scenario.confidenceScore
        
        // 1. Evidence Impact (Weighted by persona rules: Outcomes > Community > Predictions)
        val impact = when (type) {
            EvidenceType.HATCH_OUTCOME -> 0.40f * weight
            EvidenceType.TRAIT_CONFIRMATION -> 0.25f * weight
            EvidenceType.COMMUNITY_VALIDATION -> 0.15f * weight
        }

        // 2. Bayesian-style update (Simplified: New = Current + (Target - Current) * LearningRate)
        // Target is 1.0 for supporting evidence, 0.0 for conflicting (though here we assume supporting for the 'delta')
        val target = 1.0f
        val delta = (target - current) * impact
        
        val newScore = (current + delta).coerceIn(0.0f, 1.0f)
        return Pair(newScore, delta)
    }

    /**
     * Identifies traits that frequently deviate from predictions.
     */
    fun updateVolatility(
        scenario: BreedingScenario,
        traitId: String,
        isMatch: Boolean
    ): Map<String, Float> {
        val currentVolatility = scenario.volatilityIndicators[traitId] ?: 0.5f
        // If it's a match, volatility decreases slightly. If conflict, it spikes.
        val adjustment = if (isMatch) -0.05f else 0.15f
        
        val updatedMap = scenario.volatilityIndicators.toMutableMap()
        updatedMap[traitId] = (currentVolatility + adjustment).coerceIn(0.0f, 1.0f)
        return updatedMap
    }

    private fun generateHatchyRevisionNote(
        type: EvidenceType,
        delta: Float,
        promotions: List<String>
    ): String {
        val base = when (type) {
            EvidenceType.HATCH_OUTCOME -> "Now, you listen here! I've been watching those new chicks, and it looks like our theories are startin' to take shape. Nature's showin' her hand."
            EvidenceType.TRAIT_CONFIRMATION -> "You've seen it with your own eyes, and that's the best evidence there is. I'm shiftin' my confidence ranges based on your report."
            EvidenceType.COMMUNITY_VALIDATION -> "The community of breeders is weighin' in, and their collective wisdom suggests we're on the right track with these birds."
        }
        
        val promotionText = if (promotions.isNotEmpty()) { 
            " I'm feelin' quite sure about ${promotions.joinToString()} now, so I've marked 'em as stable features in the plan."
        } else ""
        
        return "$base My confidence in this pattern has grown by ${(delta * 100).toInt()}%. Remember though, the next hatch might still throw us a curveball. $promotionText"
    }
}

enum class EvidenceType {
    HATCH_OUTCOME,
    TRAIT_CONFIRMATION,
    COMMUNITY_VALIDATION
}

