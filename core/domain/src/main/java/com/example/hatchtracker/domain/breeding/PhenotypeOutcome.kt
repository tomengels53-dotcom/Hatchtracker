package com.example.hatchtracker.domain.breeding

/**
 * High-level phenotype outcome used for scenario scoring and UI display.
 * Supports sex-aware probabilities for sex-linked traits.
 */
data class PhenotypeOutcome(
    val traitId: String,
    val valueId: String,
    val label: String,
    val overallProbability: Double,
    val maleProbability: Double? = null,
    val femaleProbability: Double? = null
)
