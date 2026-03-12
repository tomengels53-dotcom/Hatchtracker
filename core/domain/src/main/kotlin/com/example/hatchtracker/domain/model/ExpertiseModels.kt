package com.example.hatchtracker.domain.model

/**
 * Derived reputation and expertise signals for a user.
 */
data class ExpertiseProfile(
    val userId: String,
    val level: ExpertiseLevel = ExpertiseLevel.BEGINNER,
    val badges: List<ExpertiseBadge> = emptyList(),
    val signals: ReputationSignals = ReputationSignals(),
    val lastProjectedAt: Long = System.currentTimeMillis()
)

data class ExpertiseBadge(
    val id: String,
    val type: BadgeType,
    val level: BadgeLevel = BadgeLevel.BRONZE,
    val dateEarned: Long = System.currentTimeMillis()
)

data class ReputationSignals(
    val yearsExperience: Double = 0.0,
    val programsCount: Int = 0,
    val breedingSuccessRate: Double = 0.0, // Percentage of programs meeting goals
    val consistentLineageDepth: Int = 0,   // Max generations tracked
    val totalBirdsManaged: Int = 0,
    val specializedSpecies: List<com.example.hatchtracker.model.Species> = emptyList(),
    val projectContributions: Int = 0
)

enum class ExpertiseLevel {
    BEGINNER,
    INTERMEDIATE,
    EXPERIENCED,
    EXPERT
}

enum class BadgeType {
    INCUBATION_EXPERT,
    WATERFOWL_SPECIALIST,
    GENETICS_SPECIALIST,
    HIGH_HATCH_RATE,
    LINEAGE_KEEPER
}

enum class BadgeLevel {
    BRONZE, SILVER, GOLD, PLATINUM
}
