package com.example.hatchtracker.domain.service

import com.example.hatchtracker.domain.model.ReputationSignals
import com.example.hatchtracker.domain.model.ExpertiseLevel
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Incubation
import com.example.hatchtracker.model.Flock
import com.example.hatchtracker.data.models.BreedingProgram

/**
 * Derives expertise and reputation signals from current HatchBase data.
 * Follows the "reflective community" principle: no manual reputation claims.
 */
class ExpertiseSignalService {

    fun calculateSignals(
        birds: List<Bird>,
        incubations: List<Incubation>,
        flocks: List<Flock>,
        programs: List<BreedingProgram>
    ): ReputationSignals {
        val totalBirds = birds.count { !it.deleted }
        val activeFlocks = flocks.count { it.active && !it.deleted }
        
        val yearsExp = if (flocks.isNotEmpty()) {
            val oldest = flocks.minOf { it.createdAt }
            (System.currentTimeMillis() - oldest) / (1000.0 * 60 * 60 * 24 * 365)
        } else 0.0

        val breedingSuccess = if (programs.isNotEmpty()) {
            val successful = programs.count { program ->
                program.goalProgress.any { goal -> goal.score0to100 >= 80 }
            }
            successful.toDouble() / programs.size
        } else 0.0

        val maxLineage = calculateMaxLineageDepth(birds)

        val specializedSpecies = birds.groupBy { it.species }
            .filter { it.value.size > 20 } // Example threshold for "specialist"
            .keys.toList()

        return ReputationSignals(
            yearsExperience = yearsExp,
            programsCount = programs.size,
            breedingSuccessRate = breedingSuccess,
            consistentLineageDepth = maxLineage,
            totalBirdsManaged = totalBirds,
            specializedSpecies = specializedSpecies
        )
    }

    private fun calculateMaxLineageDepth(birds: List<Bird>): Int {
        // Simplified depth calculation for service implementation
        if (birds.isEmpty()) return 0
        val generations = birds.map { it.generation }
        return generations.maxOrNull() ?: 0
    }

    fun calculateScore(signals: ReputationSignals): Int {
        return (signals.programsCount * 20) +
                (signals.consistentLineageDepth * 15) +
                (signals.breedingSuccessRate * 100).toInt() +
                (signals.projectContributions * 5)
    }

    fun determineLevel(signals: ReputationSignals): ExpertiseLevel {
        val score = calculateScore(signals)
        return when {
            score >= 250 -> ExpertiseLevel.EXPERT
            score >= 120 -> ExpertiseLevel.EXPERIENCED
            score >= 50 -> ExpertiseLevel.INTERMEDIATE
            else -> ExpertiseLevel.BEGINNER
        }
    }
}
