package com.example.hatchtracker.model.breeding

import com.example.hatchtracker.data.models.ConfidenceLevel

/**
 * Source of trait inheritance.
 */
enum class InheritedFrom {
    SIRE, DAM, BOTH, INFERRED, UNKNOWN
}

/**
 * Categorization of prediction reliability.
 */
enum class PredictionTier {
    HIGH, MEDIUM, UNSTABLE
}

/**
 * Result of a single trait prediction with probability tree support.
 */
data class TraitPrediction(
    val traitId: String,
    val trait: String,
    val inheritedFrom: InheritedFrom,
    val probability: Double,
    val tier: PredictionTier,
    val confidence: ConfidenceLevel,
    val explanation: String,
    val parentTraitRefs: List<String> = emptyList()
)
