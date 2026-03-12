package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.BirdTraitOverride
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.data.models.TraitCategory
import com.example.hatchtracker.data.models.TraitDisplayCatalog

/**
 * Utility to merge bird-level trait overrides into a base GeneticProfile.
 * Ensures that Mendelian overrides are propagated to fixedTraits for engine compatibility.
 */
object GeneticProfileMerger {

    fun merge(
        baseProfile: GeneticProfile,
        overrides: List<BirdTraitOverride>
    ): GeneticProfile = applyOverrides(baseProfile, overrides)

    fun applyOverrides(
        baseProfile: GeneticProfile,
        overrides: List<BirdTraitOverride>
    ): GeneticProfile {
        if (overrides.isEmpty()) return baseProfile

        val newTraitValues = baseProfile.traitValues.toMutableMap()
        val newFixedTraits = baseProfile.fixedTraits.toMutableSet()
        val newInferredTraits = baseProfile.inferredTraits.toMutableSet()

        overrides.forEach { traitOverride ->
            // 1. Update traitValues map
            newTraitValues[traitOverride.traitId] = traitOverride.optionId

            // 2. Add override option as a fixed trait label and remove conflicting labels for same trait.
            val trait = TraitDisplayCatalog.getTrait(traitOverride.traitId)
            val option = trait?.options?.find { it.id == traitOverride.optionId }

            if (option != null) {
                val conflictingLabels = trait.options.map { it.label.lowercase() }
                newFixedTraits.removeAll { existing ->
                    val existingLower = existing.lowercase()
                    conflictingLabels.any { existingLower.contains(it) }
                }
                newFixedTraits.add(option.label)
            }

            // Keep Mendelian overrides as explicit inferred signal in addition to fixed traits.
            if (traitOverride.category == TraitCategory.MENDELIAN) {
                newInferredTraits.removeAll { it.equals(traitOverride.traitId, ignoreCase = true) }
            }
        }

        return baseProfile.copy(
            traitValues = newTraitValues,
            fixedTraits = newFixedTraits.toList(),
            inferredTraits = newInferredTraits.toList(),
            traitOverrides = overrides // Keep a record of the original overrides
        )
    }
}

