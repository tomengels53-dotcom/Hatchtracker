package com.example.hatchtracker.data.breeding

import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.model.GeneticSource
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.Species

/**
 * Adapter for using global BreedStandard templates as breeding candidates.
 */
data class BreedStandardSource(
    private val standard: BreedStandard,
    override val sex: Sex? = null // Default null for templates unless specified
) : GeneticSource {
    override val geneticSourceId: String get() = "BREED_${standard.id}"
    override val displayName: String get() = "${standard.name} (Global Standard)"
    override val species: Species get() = standard.species.toSpeciesOrUnknown()
    override val geneticProfile: GeneticProfile get() = standard.geneticProfile ?: GeneticProfile()
}

private fun String.toSpeciesOrUnknown(): Species =
    runCatching { Species.valueOf(trim().uppercase()) }.getOrDefault(Species.UNKNOWN)

