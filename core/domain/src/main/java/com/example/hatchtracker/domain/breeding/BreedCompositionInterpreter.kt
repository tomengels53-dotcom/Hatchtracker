package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.genetics.BreedContribution
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BreedCompositionInterpreter @Inject constructor() {
    
    /**
     * Derives breed composition with a 3-stage fallback:
     * 1. Parents Average (if both parents exist)
     * 2. Single Parent (if only one parent exists)
     * 3. Primary Breed (fallback to bird's own breedId)
     */
    fun deriveComposition(bird: Bird, sire: Bird?, dam: Bird?): List<BreedContribution> {
        val stored = bird.breedComposition
        val raw = if (stored.isNotEmpty()) stored else when {
            sire != null && dam != null -> average(sire.breedComposition, dam.breedComposition)
            sire != null -> sire.breedComposition
            dam != null -> dam.breedComposition
            else -> listOf(BreedContribution(bird.breedId, 1.0))
        }
        return validateAndNormalize(raw, bird.displayName)
    }

    private fun validateAndNormalize(composition: List<BreedContribution>, context: String): List<BreedContribution> {
        // 1. Filter out near-zero or malformed entries
        val filtered = composition
            .filter { it.percentage > 0.001 && it.percentage <= 1.0 }
            .toMutableList()

        if (filtered.isEmpty()) {
            println("GeneticInsightEngine: Invalid or empty composition for $context. Falling back to primary.")
            return composition // Or return a safe default if needed, but for now we trust the caller has a fallback.
        }

        // 2. Canonical Ordering (Descending percentage, then BreedId)
        filtered.sortWith(compareByDescending<BreedContribution> { it.percentage }.thenBy { it.breedId })

        // 3. Sum and Correlation
        val total = filtered.sumOf { it.percentage }
        val tolerance = 0.001
        
        return if (Math.abs(total - 1.0) <= tolerance) {
            filtered
        } else {
            println("GeneticInsightEngine: Normalizing invalid composition total ($total) for $context")
            filtered.map { it.copy(percentage = it.percentage / total) }
        }
    }

    private fun average(sireComp: List<BreedContribution>, damComp: List<BreedContribution>): List<BreedContribution> {
        val combined = mutableMapOf<String, Double>()
        
        sireComp.forEach { combined[it.breedId] = (combined[it.breedId] ?: 0.0) + (it.percentage * 0.5) }
        damComp.forEach { combined[it.breedId] = (combined[it.breedId] ?: 0.0) + (it.percentage * 0.5) }
        
        val raw = combined.map { BreedContribution(it.key, it.value) }
        return validateAndNormalize(raw, "averaging")
    }

    /**
     * Translates Breed IDs and percentages into localized, human-friendly text.
     */
    fun interpret(composition: List<BreedContribution>): String {
        if (composition.isEmpty()) return "Unknown Lineage"
        
        val sorted = composition.sortedByDescending { it.percentage }
        val primary = sorted.first()
        
        return when {
            primary.percentage >= 0.95 -> "Purebred ${primary.breedId}"
            primary.percentage >= 0.75 -> "High-percentage ${primary.breedId} cross"
            composition.size == 2 && sorted[0].percentage == 0.5 && sorted[1].percentage == 0.5 -> {
                "50/50 ${sorted[0].breedId} x ${sorted[1].breedId}"
            }
            primary.percentage >= 0.5 -> "${(primary.percentage * 100).toInt()}% ${primary.breedId} dominant cross"
            else -> "Multi-breed composite (${composition.size} breeds)"
        }
    }
}
