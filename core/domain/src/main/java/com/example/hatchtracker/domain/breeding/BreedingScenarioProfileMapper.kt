package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingProgram
import com.example.hatchtracker.model.genetics.BreedingScenarioProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BreedingScenarioProfileMapper @Inject constructor() {
    
    /**
     * Maps a Breeding Program to a GeneticInsightEngine profile.
     */
    fun mapFromProgram(program: BreedingProgram?): BreedingScenarioProfile {
        if (program == null) return neutralProfile()

        val goalType = when {
            program.name.contains("Stabilize", ignoreCase = true) -> "STABILIZATION"
            program.name.contains("Selection", ignoreCase = true) -> "TRAIT_SELECTION"
            program.name.contains("Production", ignoreCase = true) -> "PRODUCTION"
            else -> "GENERAL_IMPROVEMENT"
        }
        
        val mode = when {
            program.name.contains("Backcross", ignoreCase = true) -> "BACKCROSS"
            program.name.contains("Line", ignoreCase = true) -> "LINE_BREEDING"
            else -> "OUT_CROSS"
        }

        return BreedingScenarioProfile(
            goalType = goalType,
            breedingMode = mode,
            variabilityTolerance = if (goalType == "TRAIT_SELECTION") 0.8 else 0.3,
            preferredTimeHorizonGens = 5
        )
    }

    /**
     * Provides a neutral starting profile for logic that lacks specific goal context.
     */
    fun neutralProfile(): BreedingScenarioProfile {
        return BreedingScenarioProfile(
            goalType = "GENERAL_IMPROVEMENT",
            breedingMode = "BALANCED",
            variabilityTolerance = 0.5,
            preferredTimeHorizonGens = 3
        )
    }
}
