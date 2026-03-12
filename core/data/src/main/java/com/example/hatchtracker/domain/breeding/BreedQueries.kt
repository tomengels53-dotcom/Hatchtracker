package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.Breed

enum class BreedRankingGoal {
    EGG_PRODUCTION,
    HARDINESS,
    SHOW_QUALITY
}

data class ComparisonResult(
    val breedA: Breed,
    val breedB: Breed,
    val differences: List<String>
)

object BreedQueries {

    /**
     * Filters breeds based on climate suitability (e.g., "Cold Hardy", "Heat Tolerant").
     */
    fun filterByClimate(breeds: List<Breed>, climate: String): List<Breed> {
        return breeds.filter { it.climateSuitability.contains(climate, ignoreCase = true) || it.climateSuitability.equals("Hardy", ignoreCase = true) }
    }

    /**
     * Filters breeds by their egg color.
     */
    fun filterByEggColor(breeds: List<Breed>, color: String): List<Breed> {
        return breeds.filter { it.eggColor.equals(color, ignoreCase = true) }
    }

    /**
     * Compares two breeds side-by-side and returns a summary of their differences.
     */
    fun compareBreeds(breedA: Breed, breedB: Breed): ComparisonResult {
        val differences = mutableListOf<String>()
        
        if (breedA.climateSuitability != breedB.climateSuitability) {
            differences.add("Climate: ${breedA.climateSuitability} vs ${breedB.climateSuitability}")
        }
        if (breedA.eggColor != breedB.eggColor) {
            differences.add("Egg Color: ${breedA.eggColor} vs ${breedB.eggColor}")
        }
        if (breedA.eggProduction != breedB.eggProduction) {
            differences.add("Production: ${breedA.eggProduction} vs ${breedB.eggProduction}")
        }
        
        return ComparisonResult(breedA, breedB, differences)
    }

    /**
     * Ranks breeds based on specific breeding goals.
     */
    fun rankBreedsForGoal(breeds: List<Breed>, goal: BreedRankingGoal): List<Breed> {
        return when (goal) {
            BreedRankingGoal.EGG_PRODUCTION -> breeds.sortedByDescending {
                when(it.eggProduction) {
                    "Prolific" -> 3
                    "Normal" -> 2
                    "Not Effective" -> 1
                    else -> 0
                }
            }
            BreedRankingGoal.HARDINESS -> breeds.filter { it.climateSuitability.contains("Hardy", ignoreCase = true) }
            BreedRankingGoal.SHOW_QUALITY -> breeds // Placeholder for future show metrics
        }
    }
}
