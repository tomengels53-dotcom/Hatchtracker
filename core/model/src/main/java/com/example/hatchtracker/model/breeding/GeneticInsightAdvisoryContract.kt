package com.example.hatchtracker.model.breeding

import com.example.hatchtracker.model.genetics.InsightConfidence

/**
 * Formal, versioned semantic bridge between the GeneticInsightEngine 
 * and user-facing surfaces (UI/Hatchy).
 * 
 * DESIGN RULE: This model contains ONLY semantic codes and normalized values.
 * UI wording is resolved by mappers consuming this contract.
 */
data class GeneticInsightAdvisoryContract(
    val contractVersion: Int = 1,
    val insightSummaryCodes: List<String>, // Whitelisted semantic tags in prioritized order
    val topWarningCode: String = "NONE",
    val topActionCategory: String = "NONE",
    val confidenceBand: InsightConfidence = InsightConfidence.MODERATE,
    val dataStrengthTier: GeneticDataStrengthTier = GeneticDataStrengthTier.COMPACT,
    val whyUnavailableCode: String = "NONE"
) {
    companion object {
        const val WARNING_NONE = "NONE"
        const val ACTION_NONE = "NONE"
        const val UNAVAILABLE_NONE = "NONE"
    }
}

enum class GeneticDataStrengthTier {
    FULL,    // Complete pedigree and composition data
    PARTIAL, // Some data missing, but representative enough for core insights
    COMPACT  // Minimal data; only high-level notes available
}

/**
 * Whitelist of semantic codes for stable UI/Hatchy mapping.
 */
object GeneticAdvisoryCodes {
    // Summaries
    const val HIGH_VARIABILITY_F2 = "HIGH_VARIABILITY_F2"
    const val PREMATURE_FIXATION = "PREMATURE_FIXATION"
    const val STABILIZATION_LAG = "STABILIZATION_LAG"
    const val HYBRID_VIGOR_PRESENT = "HYBRID_VIGOR_PRESENT"
    const val LIMITED_INTELLIGENCE = "LIMITED_INTELLIGENCE"
    const val NO_STRONG_SIGNAL = "NO_STRONG_SIGNAL"

    // Warnings
    const val INBREEDING_RISK = "INBREEDING_RISK"
    const val TRAIT_INSTABILITY = "TRAIT_INSTABILITY"

    // Unavailability
    const val MISSING_PEDIGREE = "MISSING_PEDIGREE"
    const val MISSING_BREED_COMPOSITION = "MISSING_BREED_COMPOSITION"
    const val NO_ACTIVE_SCENARIO = "NO_ACTIVE_SCENARIO"
    const val LOW_CONFIDENCE_METADATA = "LOW_CONFIDENCE_METADATA"
}
