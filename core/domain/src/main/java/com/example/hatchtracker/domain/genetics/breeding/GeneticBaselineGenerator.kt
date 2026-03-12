package com.example.hatchtracker.domain.genetics.breeding

import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.model.Species

/**
 * Utility to auto-generate a baseline genetic profile for a breed.
 * Maps physical characteristics to standardized genetic tokens.
 */
object GeneticBaselineGenerator {

    /**
     * Generates a baseline [GeneticProfile] if the existing one is empty or incomplete.
     */
    fun generateBaseline(standard: BreedStandard): GeneticProfile {
        val currentProfile = standard.geneticProfile
        
        // Only generate if we don't have official fixed traits already
        if (currentProfile.fixedTraits.isNotEmpty()) return currentProfile

        val inferredTraits = mutableSetOf<String>()
        val species = try {
            Species.valueOf(standard.species.uppercase())
        } catch (_: Exception) {
            Species.CHICKEN
        }
        
        // 1. Species-Specific Heuristics
        if (species == Species.CHICKEN) {
            // Comb Type Genetics
            when (standard.combType.lowercase()) {
                "pea" -> inferredTraits.add("pea comb")
                "rose" -> inferredTraits.add("rose comb")
                "walnut" -> {
                    inferredTraits.add("pea comb")
                    inferredTraits.add("rose comb")
                }
                "v-shape" -> inferredTraits.add("v-comb")
            }

            // Feather Type
            when (standard.featherType.lowercase()) {
                "frizzle" -> inferredTraits.add("frizzle")
                "silkied" -> inferredTraits.add("silkie feathers")
                "feather-footed" -> inferredTraits.add("feathered shanks")
            }

            // Skin Color
            if (standard.skinColor.lowercase() == "black") {
                inferredTraits.add("fibromelanosis")
            }

            // Egg Color
            if (standard.eggColor.contains("blue", ignoreCase = true) || 
                standard.eggColor.contains("green", ignoreCase = true)) {
                inferredTraits.add("blue eggs")
            }

            // Size/Category
            if (standard.isTrueBantam) {
                inferredTraits.add("dwarfism")
            }
        } else {
            // Non-chicken Heuristics (e.g. DUCK white color)
            if (standard.acceptedColors.any { it.lowercase() == "white" }) {
                inferredTraits.add("white plumage")
            }
        }

        val finalInferred = (currentProfile.inferredTraits + inferredTraits).distinct()

        return currentProfile.copy(
            inferredTraits = finalInferred,
            confidenceLevel = ConfidenceLevel.LOW.name
        )
    }
}

