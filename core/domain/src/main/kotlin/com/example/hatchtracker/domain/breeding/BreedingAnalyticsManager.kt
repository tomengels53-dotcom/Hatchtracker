package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Incubation
import com.example.hatchtracker.model.GeneticProfile

data class TraitProgressionPoint(
    val generation: Int,
    val prevalence: Float, // 0.0 to 1.0
    val traitName: String
)

data class ConfidencePoint(
    val timestamp: Long,
    val averageConfidence: Float // 0.0 to 1.0
)

data class BreedingPairStats(
    val sireId: Long,
    val damIds: List<Long>,
    val hatchRate: Float,
    val incubationCount: Int
)

object BreedingAnalyticsManager {

    /**
     * Calculates hatch success rates for a specific pair or aggregate.
     */
    fun calculatePairStats(
        sireId: Long,
        damIds: List<Long>,
        incubations: List<Incubation>
    ): BreedingPairStats {
        val relevantIncubations = incubations.filter { 
            it.fatherBirdId == sireId && it.birdId in damIds && it.hatchCompleted 
        }
        
        if (relevantIncubations.isEmpty()) return BreedingPairStats(sireId, damIds, 0f, 0)
        
        val totalEggs = relevantIncubations.sumOf { it.eggsCount }
        val totalHatched = relevantIncubations.sumOf { it.hatchedCount }
        
        val rate = if (totalEggs > 0) totalHatched.toFloat() / totalEggs else 0f
        
        return BreedingPairStats(sireId, damIds, rate, relevantIncubations.size)
    }

    /**
     * Maps trait frequencies across generations (F0 -> F1 -> F2).
     */
    fun getTraitProgression(
        birds: List<Bird>,
        traitName: String
    ): List<TraitProgressionPoint> {
        val generations = birds.map { it.generation }.distinct().sorted()
        
        return generations.map { gen ->
            val genBirds = birds.filter { it.generation == gen }
            val withTrait = genBirds.count { 
                it.geneticProfile.fixedTraits.contains(traitName) || 
                it.geneticProfile.inferredTraits.contains(traitName) 
            }
            val prevalence = if (genBirds.isNotEmpty()) withTrait.toFloat() / genBirds.size else 0f
            
            TraitProgressionPoint(gen, prevalence, traitName)
        }
    }

    /**
     * Tracks average confidenceLevel improvements over time.
     */
    fun getConfidenceGrowthTrend(
        birds: List<Bird>
    ): List<ConfidencePoint> {
        // Sort birds by hatch date or creation time to show "Time" progression
        // For simplicity, we'll use lastUpdated as a proxy for when the data was "finalized"
        val sortedBirds = birds.sortedBy { it.lastUpdated }
        
        val confidenceMap = mapOf("HIGH" to 1.0f, "MEDIUM" to 0.6f, "LOW" to 0.3f)
        
        return sortedBirds.map { bird ->
            val score = confidenceMap[bird.geneticProfile.confidenceLevel] ?: 0.3f
            ConfidencePoint(bird.lastUpdated, score)
        }
    }
    
    /**
     * Identifies the most impactful traits for a species.
     */
    fun getTopTraits(birds: List<Bird>, species: String): List<String> {
        return birds.filter { it.species.name.equals(species, ignoreCase = true) }
            .flatMap { it.geneticProfile.fixedTraits + it.geneticProfile.inferredTraits }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }
}

