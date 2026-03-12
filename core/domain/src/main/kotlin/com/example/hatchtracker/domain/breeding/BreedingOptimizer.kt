package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.*

object BreedingOptimizer {

    fun optimize(
        activeBirds: List<Bird>,
        breedingHistory: List<BreedingRecord>,
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
                if (male.species != female.species) return@forEach

                // 1. Ancestry Check
                if (AncestryService.hasConflict(male, female, birdMap = birdMap)) {
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
        history: List<BreedingRecord>,
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
        val diversityScore = (1.0f - jaccard)
        
        baseScore += (diversityScore * 30 * weights.diversityFocus)
        if (diversityScore > 0.8) rationales.add("High genetic diversity pair.")

        val avgConfidence = (male.geneticProfile.confidenceLevelEnum.weight + female.geneticProfile.confidenceLevelEnum.weight) / 2.0

        return ScoreResult(
            totalScore = baseScore,
            confidence = avgConfidence.toFloat(),
            diversity = diversityScore,
            warnings = warnings,
            traits = maleGenes.union(femaleGenes).toList().take(3),
            rationale = rationales.joinToString(", ")
        )
    }

    fun runMultiGenBeamSearch(
        initialPopulation: List<Bird>,
        goals: List<BreedingGoal>,
        maxGens: Int = 3,
        beamWidth: Int = 5
    ): List<BreedingPath> {
        var currentBeam = listOf(BreedingPath(initialPopulation, emptyList(), 0f))
        val birdMap = initialPopulation.associateBy { it.localId }

        repeat(maxGens) { gen ->
            val nextCandidates = mutableListOf<BreedingPath>()
            
            currentBeam.forEach { path ->
                val population = path.currentPopulation
                val males = population.filter { it.sex == Sex.MALE }
                val females = population.filter { it.sex == Sex.FEMALE }

                males.forEach { male ->
                    females.forEach { female ->
                        if (male.species != female.species) return@forEach
                        
                        if (AncestryService.hasConflict(male, female, birdMap)) return@forEach
                        
                        val virtualOffspring = simulateOffspring(male, female, gen + 1)
                        
                        val newPath = path.copy(
                            currentPopulation = population + virtualOffspring,
                            steps = path.steps + BreedingStep(male, female, virtualOffspring),
                            score = calculatePathScore(path.steps + BreedingStep(male, female, virtualOffspring), goals)
                        )
                        nextCandidates.add(newPath)
                    }
                }
            }

            currentBeam = nextCandidates
                .distinctBy { it.steps.last().offspring.syncId }
                .sortedByDescending { it.score }
                .take(beamWidth)
        }

        return currentBeam
    }

    private fun simulateOffspring(male: Bird, female: Bird, gen: Int): Bird {
        val virtualTraits = (male.geneticProfile.fixedTraits + female.geneticProfile.fixedTraits).distinct()

        return Bird(
            localId = -(System.currentTimeMillis() % 1000000 + (gen * 1000).toLong()), // Virtual ID
            syncId = "virtual_${gen}_${male.localId}_${female.localId}",
            species = male.species,
            breed = "${male.breed} x ${female.breed} F$gen",
            sex = if (gen % 2 == 0) Sex.MALE else Sex.FEMALE,
            hatchDate = "SIMULATED",
            motherId = female.localId,
            fatherId = male.localId,
            geneticProfile = male.geneticProfile.copy(
                fixedTraits = virtualTraits,
                inferredTraits = emptyList(),
                confidenceLevel = ConfidenceLevel.MEDIUM.name
            )
        )
    }

    private fun calculatePathScore(steps: List<BreedingStep>, goals: List<BreedingGoal>): Float {
        val lastOffspring = steps.lastOrNull()?.offspring ?: return 0f
        var score = 0f
        goals.forEach { goal ->
            val targetValue = goal.targetValue
            if (targetValue != null &&
                (lastOffspring.geneticProfile.fixedTraits.contains(targetValue) ||
                    lastOffspring.breed.contains(targetValue, ignoreCase = true))
            ) {
                score += 100f * goal.priority
            }
        }
        score -= (steps.size * 5f)
        return score
    }
}
