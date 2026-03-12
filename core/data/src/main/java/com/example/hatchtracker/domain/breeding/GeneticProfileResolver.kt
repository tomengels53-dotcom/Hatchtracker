package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.data.repository.BreedRepository
import com.example.hatchtracker.model.Species
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Singleton

@Singleton
class GeneticProfileResolver constructor(
    private val breedRepository: BreedRepository? = null
) {
    /**
     * Resolves the effective genetic profile for a bird.
     * Priority:
     * 1. Bird's custom profile (if Mixed/Other and set)
     * 2. Flock's default profile (if Mixed/Other and set)
     * 3. Breed Standard (if Purebred)
     */
    suspend fun resolveGeneticProfile(bird: Bird, flock: Flock?): GeneticProfile {
        // 1. Determine the Base Profile (Custom > Flock > Breed)
        var baseProfile = GeneticProfile()
        var foundBase = false

        // A. Bird profile
        val birdProfile = bird.geneticProfile
        if (birdProfile.traitValues.isNotEmpty()) {
            baseProfile = birdProfile
            foundBase = true
        }

        // B. Flock Default (Inherited Mixed Profile)
        if (!foundBase) {
            val defaultFlockProfile = flock?.defaultGeneticProfile
            if (defaultFlockProfile != null && defaultFlockProfile.traitValues.isNotEmpty()) {
                baseProfile = defaultFlockProfile
                foundBase = true
            }
        }

        // C. Breed Default (Pure breed)
        if (!foundBase && !isMixedBreed(bird.breed) && bird.species != Species.UNKNOWN) {
            try {
                val standard = breedRepository?.getBreedStandard(bird.species.name, bird.breed)?.firstOrNull()
                if (standard != null) {
                    val traitMap = standard.geneticProfile.traitValues.toMutableMap()
                    val species = bird.species

                    if (!traitMap.containsKey("egg_color") && standard.eggColor.isNotBlank()) {
                        traitMap["egg_color"] = mapEggColor(standard.eggColor)
                    }
                    if (species == Species.CHICKEN) {
                        if (!traitMap.containsKey("comb") && standard.combType.isNotBlank()) {
                            traitMap["comb"] = standard.combType
                        }
                    }
                    baseProfile = standard.geneticProfile.copy(traitValues = traitMap)
                    foundBase = true
                }
            } catch (e: Exception) { /* Fallback to empty */ }
        }

        // 2. Apply Bird-Level Overrides (The new Phase 3 system)
        val overrides = bird.geneticProfile.traitOverrides
        return if (overrides.isNotEmpty()) {
            GeneticProfileMerger.applyOverrides(baseProfile, overrides)
        } else {
            baseProfile
        }
    }
    
    private fun isMixedBreed(breed: String): Boolean {
        return breed.equals("Mixed", ignoreCase = true) || 
               breed.equals("Mixed/Other", ignoreCase = true) || 
               breed.equals("Other", ignoreCase = true) || 
               breed.equals("Unknown", ignoreCase = true)
    }
    
    private fun mapEggColor(colorName: String): String {
        return when {
            colorName.contains("Blue", ignoreCase = true) -> "blue"
            colorName.contains("Green", ignoreCase = true) -> "green"
            colorName.contains("Olive", ignoreCase = true) -> "olive"
            colorName.contains("Dark Brown", ignoreCase = true) || colorName.contains("Chocolate", ignoreCase = true) -> "dark_brown"
            colorName.contains("Brown", ignoreCase = true) -> "brown"
            colorName.contains("White", ignoreCase = true) -> "white"
            colorName.contains("Cream", ignoreCase = true) || colorName.contains("Tinted", ignoreCase = true) -> "cream"
            else -> "unknown"
        }
    }
}

