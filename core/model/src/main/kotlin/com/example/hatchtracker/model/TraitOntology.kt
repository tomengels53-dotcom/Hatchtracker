package com.example.hatchtracker.model

/**
 * Normalized dimensions for breed-field lookups.
 * Maps KnowledgeTopics to specific BreedStandard fields.
 */
enum class TraitDimension {
    EGG_COLOR,
    EGG_PRODUCTION_RATE,
    BROODINESS_LEVEL,
    TEMPERAMENT_DOCILITY,
    COLD_HARDINESS,
    HEAT_TOLERANCE,
    BODY_SIZE,
    MEAT_YIELD,
    PLUMAGE_COLOR,
    COMB_TYPE,
    WATER_BEHAVIOUR,
    GUARDING_BEHAVIOUR,
    EARLY_MATURITY,
    FLOCK_COMPATIBILITY,
    FORAGING_ABILITY,
    CONFINEMENT_TOLERANCE,
    DISEASE_RESISTANCE,
    MOTHERING_ABILITY,
    NOISE_LEVEL
}

/**
 * Base interface for all normalized trait values.
 */
sealed interface TraitValue

enum class EggColor : TraitValue {
    BLUE, GREEN, BROWN, WHITE, CREAM, DARK_BROWN, SPECKLED, OLIVE
}

enum class TemperamentLevel : TraitValue {
    DOCILE, ACTIVE, CALM, AGGRESSIVE, SKITTISH
}

enum class HardinessLevel : TraitValue {
    COLD_HARDY, HEAT_TOLERANT, ROBUST, SENSITIVE
}

enum class BodySizeClass : TraitValue {
    BANTAM, LIGHT, MEDIUM, HEAVY, EXTRA_HEAVY
}

enum class PrimaryUsage : TraitValue {
    LAYER, MEAT, DUAL_PURPOSE, ORNAMENTAL, CONSERVATION
}

enum class BroodinessLevel : TraitValue {
    NEVER, SELDOM, OCCASIONAL, FREQUENT, PERSISTENT
}

enum class CombType : TraitValue {
    SINGLE, PEA, ROSE, WALNUT, V_SHAPE, BUTTERCUP, CUSHION, STRAWBERRY, NONE
}

/**
 * A specific constraint used for filtering BreedStandardRepository.
 */
data class TraitConstraint(
    val dimension: TraitDimension,
    val value: TraitValue
)
