package com.example.hatchtracker.domain.breeding

/**
 * Snapshot of line stability for a specific generation.
 */
data class LineStabilitySnapshot(
    val glsiScore: Int, // 0 - 100
    val components: StabilityComponents,
    val drivers: List<StabilityDriver>,
    val banner: StabilityWarningBanner? = null,
    val isEstablished: Boolean = false,
    val explanation: List<String> = emptyList(),
    val fixationProgress: Double = 0.0,
    val effectivePopulation: Double = 0.0
)

data class StabilityComponents(
    val fixation: Double,   // 0 - 1
    val variance: Double,   // 0 - 1
    val risk: Double,       // 0 - 1
    val diversity: Double,  // 0 - 1
    val confidence: Double  // 0 - 1
)

data class StabilityDriver(
    val title: String,
    val detail: String,
    val impact: DriverImpact,
    val weight: Double,
    val delta: Double
)

enum class DriverImpact {
    POSITIVE, NEGATIVE, WARNING
}

data class StabilityWarningBanner(
    val title: String,
    val body: String,
    val ctaPrimary: String? = null,
    val ctaSecondary: String? = null
)

data class DiversityMeta(
    val uniqueSires: Int,
    val repeatedSireCount: Int,
    val totalBirds: Int
)
