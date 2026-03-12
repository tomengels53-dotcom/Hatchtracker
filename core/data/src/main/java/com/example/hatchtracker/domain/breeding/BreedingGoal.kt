package com.example.hatchtracker.domain.breeding

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
    val targetEggColor: String? = null,
    val climate: String = "temperate", // temperate, cold, hot
    val sizePreference: String = "medium" // small, medium, large
)
