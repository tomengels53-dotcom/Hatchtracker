package com.example.hatchtracker.model

/**
 * Common interface for all Hatchy topics used in intent resolution and data mapping.
 */
sealed interface HatchyTopic

/**
 * Topics related to poultry knowledge, traits, and specific biological characteristics.
 * Relocated to :core:model to allow BreedStandard and SpeciesTraitProfile to use them.
 */
enum class KnowledgeTopic : HatchyTopic {
    CROSSBREED_RECOMMENDATION,
    TRAIT_INHERITANCE,
    GENERATION_VARIATION,
    BREEDING_STRATEGY,
    GOAL_BASED_PAIRING,
    SETUP_DEVICE,
    TEMPERATURE,
    HUMIDITY,
    TURNING,
    LOCKDOWN,
    HATCH_TIMING,
    BROODER_TEMPERATURE,
    READY_TO_MOVE,
    CALIBRATE_DEVICE,
    CLEAN_DEVICE,
    MAINTENANCE_DUE,
    EARLY_CHICK_CARE,
    INCUBATION_PERIOD,
    
    // --- Phase B: Trait-Specific Topics ---
    EGG_TRAITS,
    TEMPERAMENT,
    HARDINESS,
    UTILITY_PURPOSE,
    PHYSICAL_TRAITS,
    HEALTH_ROBUSTNESS,
    
    // Granular Ontology Topics
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
    NOISE_LEVEL,
    MOTHERING_ABILITY
}

/**
 * Topics related to application workflows and actions.
 */
enum class WorkflowTopic : HatchyTopic {
    START_INCUBATION,
    LOG_EXPENSE,
    ADD_EQUIPMENT,
    RECORD_HATCH
}

/**
 * Topics related to data summaries and statistics.
 */
enum class DataTopic : HatchyTopic {
    ACTIVE_BATCH_STATUS,
    TOTAL_SPEND,
    LOSSES_SUMMARY,
    CATEGORY_BREAKDOWN,
    ACTIVE_CHICK_COUNT,
    AGE_GROUP_SUMMARY,
    MONTHLY_TREND,
    FLOCK_COST,
    SENSOR_STATUS,
    ACTIVE_DEVICES
}
