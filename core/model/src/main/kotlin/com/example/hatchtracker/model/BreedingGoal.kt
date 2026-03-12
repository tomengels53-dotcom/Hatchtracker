package com.example.hatchtracker.model

enum class BreedingGoalType {
    EGG_COLOR,
    SIZE,
    TEMPERAMENT,
    GENETIC_DIVERSITY,
    BREED_STABILIZATION,
    SEX_LINKED_SORTING,
    EXOTIC_FEATURES
}

data class BreedingGoal(
    val type: BreedingGoalType,
    val targetValue: String? = null,
    val priority: Int = 1, // 1 (Low) to 5 (High)
    val inbreedingTolerance: String = "low", // low, medium, high
    val targetEggColor: EggColor? = null,
    val climate: HardinessLevel? = null,
    val sizePreference: BodySizeClass? = null
)

