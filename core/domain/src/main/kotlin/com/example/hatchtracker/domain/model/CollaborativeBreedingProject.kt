package com.example.hatchtracker.domain.model

/**
 * A collaborative breeding initiative where multiple users contribute derived data
 * to track shared breeding goals and performance.
 */
data class CollaborativeBreedingProject(
    val projectId: String,
    val species: com.example.hatchtracker.model.Species,
    val breedFocus: String,
    val title: String,
    val goal: String,
    val generationTrackingEnabled: Boolean = true,
    val participants: List<ProjectParticipant> = emptyList(),
    val aggregatedMetrics: ProjectMetrics = ProjectMetrics(),
    val createdAt: Long = System.currentTimeMillis(),
    val status: ProjectStatus = ProjectStatus.OPEN
)

data class ProjectParticipant(
    val userId: String,
    val username: String,
    val role: ParticipantRole = ParticipantRole.CONTRIBUTOR,
    val personalContributionCount: Int = 0,
    val joinedAt: Long = System.currentTimeMillis()
)

data class ProjectMetrics(
    val averageHatchRate: Double = 0.0,
    val totalOffspringTracked: Int = 0,
    val traitConsistencyScore: Int = 0, // 0-100
    val productionEfficiencyGain: Double = 0.0, // Comparison with baseline
    val fertilityRate: Double = 0.0
)

enum class ParticipantRole {
    OWNER,      // Project creator
    ELITE,      // High-expertise contributor
    CONTRIBUTOR // Standard participant
}

enum class ProjectStatus {
    DRAFT,      // Initial setup
    OPEN,       // Accepting new participants
    ACTIVE,     // Focus phase
    PAUSED,     // Temporarily halted
    COMPLETED,  // Goal reached, results finalized
    ARCHIVED    // Closed
}
