package com.example.hatchtracker.data.models

/**
 * Defines the target sex for a trait requirement.
 */
enum class TargetSex { BOTH, MALE, FEMALE }

/**
 * Defines a specific phenotypic target for a trait.
 */
data class TraitTarget(
    val traitId: String,
    val valueId: String,
    val weight: Double = 1.0,
    val appliesToSex: TargetSex = TargetSex.BOTH
)

/**
 * Defines the overall breeding goal for a scenario.
 */
data class BreedingTarget(
    val requiredTraits: List<TraitTarget> = emptyList(),
    val preferredTraits: List<TraitTarget> = emptyList(),
    val excludedTraits: List<TraitTarget> = emptyList(),
    val generations: Int = 1,
    val maleRatio: Double = 0.5,
    val femaleRatio: Double = 0.5
)

enum class StartingSituation {
    COMBINE_FLOCKS,
    IMPROVE_FLOCK,
    START_FROM_SCRATCH
}

enum class StrategyMode {
    STRICT_LINE_BREEDING,
    COMMERCIAL_PRODUCTION
}

enum class TraitDomain {
    EGG_TRAITS,
    PLUMAGE_TRAITS,
    STRUCTURAL_TRAITS,
    PRODUCTIVITY,
    BEHAVIOR,
    GENETIC_STABILITY
}

data class GoalSpec(
    val domain: TraitDomain,
    val traitKey: String,
    val targetValue: String, // Simplified Any to String for persistence
    val priority: Int = 3,
    val confidencePreference: Int = 3 // 1-5 scale
)

enum class EstimateConfidence {
    LOW,
    MED,
    HIGH;

    companion object {
        fun fromNumeric(value: Double): EstimateConfidence = when {
            value > 0.8 -> HIGH
            value > 0.5 -> MED
            else -> LOW
        }
    }
}

data class ConfidenceBreakdown(
    val evidence: Double,
    val model: Double,
    val planComplexity: Double,
    val topRiskFactors: List<String>
)

data class GenEstimate(
    val minGenerations: Int,
    val maxGenerations: Int,
    val confidence: EstimateConfidence,
    val limitingFactors: List<String>,
    val breakdownByStage: Map<String, GenEstimate>? = null,
    val confidenceBreakdown: ConfidenceBreakdown? = null
) {
    val numericConfidence: Double
        get() = confidenceBreakdown?.let {
            java.lang.Math.pow(it.evidence * it.model * it.planComplexity, 1.0 / 3.0).coerceIn(0.35, 0.98)
        } ?: when (confidence) {
            EstimateConfidence.HIGH -> 0.95
            EstimateConfidence.MED -> 0.70
            EstimateConfidence.LOW -> 0.40
        }
}

data class StrategyConfig(
    val startingSituation: StartingSituation = StartingSituation.COMBINE_FLOCKS,
    val goalSpecs: List<GoalSpec> = emptyList(),
    val strategyMode: StrategyMode = StrategyMode.STRICT_LINE_BREEDING
) {
    fun hasDomain(domain: TraitDomain): Boolean = goalSpecs.any { it.domain == domain }
    fun primaryGoals(): List<GoalSpec> = goalSpecs.filter { it.priority >= 4 }
}
