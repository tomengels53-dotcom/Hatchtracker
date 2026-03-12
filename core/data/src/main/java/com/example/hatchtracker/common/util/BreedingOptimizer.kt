package com.example.hatchtracker.common.util

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Incubation
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.domain.breeding.BreedingGoal
import com.example.hatchtracker.domain.breeding.BreedingGoalType

data class RecommendedPair(
    val male: Bird,
    val female: Bird,
    val totalScore: Float,
    val confidenceScore: Float, // 0.0 - 1.0
    val diversityScore: Float, // 0.0 - 1.0
    val warnings: List<String>,
    val predictedTraits: List<String>,
    val rationale: String
)

data class OptimizationWeights(
    val traitFocus: Float = 1.0f,
    val diversityFocus: Float = 1.0f,
    val performanceFocus: Float = 1.0f
)

object BreedingOptimizer {

    fun optimize(
        activeBirds: List<Bird>,
        breedingHistory: List<com.example.hatchtracker.data.models.BreedingRecord>,
        incubations: List<Incubation>,
        goals: List<BreedingGoal>,
        weights: OptimizationWeights = OptimizationWeights()
    ): List<RecommendedPair> {
        val males = activeBirds.filter { it.sex == Sex.MALE }
        val females = activeBirds.filter { it.sex == Sex.FEMALE }
        
        // Build efficient ancestor map for faster lookups
        val birdMap = activeBirds.associateBy { it.localId }

        val rankedPairs = mutableListOf<RecommendedPair>()

        males.forEach { male ->
            females.forEach { female ->
                // Skip if different species
                if (male.species != female.species) return@forEach

                // 1. Ancestry Check (Hard Constraint)
                if (AncestryService.hasConflict(male, female, birdMap)) {
                    // Skip completely or add with negative score if you want to show "Conflicted"
                    return@forEach 
                }

                // 2. Calculate Scores
                val scores = calculateScores(male, female, goals, breedingHistory, incubations, weights)
                
                if (scores.totalScore > 0) {
                     rankedPairs.add(RecommendedPair(
                        male = male,
                        female = female,
                        totalScore = scores.totalScore,
                        confidenceScore = scores.confidence,
                        diversityScore = scores.diversity,
                        warnings = scores.warnings,
                        predictedTraits = scores.traits,
                        rationale = scores.rationale
                    ))
                }
            }
        }
        
        return rankedPairs.sortedByDescending { it.totalScore }
    }

    private data class ScoreResult(
        val totalScore: Float,
        val confidence: Float,
        val diversity: Float,
        val warnings: List<String>,
        val traits: List<String>,
        val rationale: String
    )

    private fun calculateScores(
        male: Bird,
        female: Bird,
        goals: List<BreedingGoal>,
        history: List<com.example.hatchtracker.data.models.BreedingRecord>,
        incubations: List<Incubation>,
        weights: OptimizationWeights
    ): ScoreResult {
        var baseScore = 50f
        val warnings = mutableListOf<String>()
        val rationales = mutableListOf<String>()
        val traits = mutableListOf<String>()

        // --- A. Trait Goal Score ---
        var traitScore = 0f
        goals.forEach { goal ->
            // Re-use Logic from BreedingRecommender but scaled with confidence
            val maleTraits = male.geneticProfile.fixedTraits + male.geneticProfile.inferredTraits
            val femaleTraits = female.geneticProfile.fixedTraits + female.geneticProfile.inferredTraits
            
            val matchQuality = when(goal.type) {
                BreedingGoalType.EGG_COLOR -> if (maleTraits.any { it.contains("Egg") } || femaleTraits.any { it.contains("Egg") }) 1.0f else 0f
                BreedingGoalType.SIZE -> if (maleTraits.any { it.contains("Size") } || femaleTraits.any { it.contains("Size") }) 1.0f else 0f
                else -> 0f
            }
            
            if (matchQuality > 0) {
                val confWeight = (male.geneticProfile.confidenceLevelEnum.weight + female.geneticProfile.confidenceLevelEnum.weight) / 2.0
                traitScore += (20 * goal.priority * matchQuality * confWeight).toFloat()
                rationales.add("Matches ${goal.type}")
            }
        }
        baseScore += (traitScore * weights.traitFocus)

        // --- B. Genetic Diversity (Jaccard) ---
        val maleGenes = (male.geneticProfile.knownGenes + male.geneticProfile.fixedTraits).toSet()
        val femaleGenes = (female.geneticProfile.knownGenes + female.geneticProfile.fixedTraits).toSet()
        val intersection = maleGenes.intersect(femaleGenes).size
        val union = maleGenes.union(femaleGenes).size
        val jaccard = if (union > 0) intersection.toFloat() / union else 0f
        val diversityScore = (1.0f - jaccard) // Higher is distinct
        
        baseScore += (diversityScore * 30 * weights.diversityFocus)
        if (diversityScore > 0.8) rationales.add("High genetic diversity pair.")

        // --- C. Confidence ---
        val avgConfidence = (male.geneticProfile.confidenceLevelEnum.weight + female.geneticProfile.confidenceLevelEnum.weight) / 2.0

        return ScoreResult(
            totalScore = baseScore,
            confidence = avgConfidence.toFloat(),
            diversity = diversityScore,
            warnings = warnings,
            traits = maleGenes.union(femaleGenes).toList().take(3), // Just sample traits
            rationale = rationales.joinToString(", ")
        )
    }
}

