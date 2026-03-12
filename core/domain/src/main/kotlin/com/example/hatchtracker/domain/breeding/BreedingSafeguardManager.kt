package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.BreedingSafeguard

/**
 * Domain-layer manager for breeding safety and welfare.
 * Pure logic, no UI dependencies.
 */
object BreedingSafeguardManager {

    fun evaluatePair(sire: Bird?, dams: List<Bird>, birdMap: Map<Long, Bird>): BreedingSafeguard {
        if (sire == null || dams.isEmpty()) return BreedingSafeguard.None

        // 1. Extreme Risk: Lethal Gene Combinations
        if (isLethalCombination(sire, dams)) {
            return BreedingSafeguard.BlockingLethal
        }

        // 2. High Risk: Severe Inbreeding (COI-based)
        val maxInbreeding = dams.maxOf { dam ->
            AncestryService.calculateCOI(sire, dam, birdMap)
        }
        
        if (maxInbreeding > 0.25f) {
            return BreedingSafeguard.WarningInbreeding
        }

        return BreedingSafeguard.None
    }

    private fun isLethalCombination(sire: Bird, dams: List<Bird>): Boolean {
        // Known lethal combinations (homozygous dominant lethality)
        val lethalBreeds = listOf("Creeper", "Manx", "Japanese Bantam")
        
        return dams.any { dam ->
            lethalBreeds.any { breed ->
                sire.breed.contains(breed, ignoreCase = true) && 
                dam.breed.contains(breed, ignoreCase = true)
            }
        }
    }
}


