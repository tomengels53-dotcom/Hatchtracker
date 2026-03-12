package com.example.hatchtracker.model

/**
 * Result of a breeding pair optimization.
 */
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

/**
 * Configuration weights for the Breeding Optimizer.
 */
data class OptimizationWeights(
    val traitFocus: Float = 1.0f,
    val diversityFocus: Float = 1.0f,
    val performanceFocus: Float = 1.0f
)

data class BreedingStep(
    val sire: Bird,
    val dam: Bird,
    val offspring: Bird
)

data class BreedingPath(
    val currentPopulation: List<Bird>,
    val steps: List<BreedingStep>,
    val score: Float
)
