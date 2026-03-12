package com.example.hatchtracker.data.models

import com.google.firebase.firestore.PropertyName

data class CollaborativeProjectDto(
    @get:PropertyName("projectId") @set:PropertyName("projectId") var projectId: String = "",
    @get:PropertyName("species") @set:PropertyName("species") var species: String = "",
    @get:PropertyName("breedFocus") @set:PropertyName("breedFocus") var breedFocus: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("goal") @set:PropertyName("goal") var goal: String = "",
    @get:PropertyName("genTracking") @set:PropertyName("genTracking") var generationTrackingEnabled: Boolean = true,
    @get:PropertyName("participants") @set:PropertyName("participants") var participants: List<ProjectParticipantDto> = emptyList(),
    @get:PropertyName("metrics") @set:PropertyName("metrics") var metrics: ProjectMetricsDto = ProjectMetricsDto(),
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = 0,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "OPEN"
)

data class ProjectParticipantDto(
    @get:PropertyName("uid") @set:PropertyName("uid") var userId: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var username: String = "",
    @get:PropertyName("role") @set:PropertyName("role") var role: String = "CONTRIBUTOR",
    @get:PropertyName("contributions") @set:PropertyName("contributions") var personalContributionCount: Int = 0,
    @get:PropertyName("joinedAt") @set:PropertyName("joinedAt") var joinedAt: Long = 0
)

data class ProjectMetricsDto(
    @get:PropertyName("hatchRate") @set:PropertyName("hatchRate") var averageHatchRate: Double = 0.0,
    @get:PropertyName("totalBirds") @set:PropertyName("totalBirds") var totalOffspringTracked: Int = 0,
    @get:PropertyName("traitScore") @set:PropertyName("traitScore") var traitConsistencyScore: Int = 0,
    @get:PropertyName("efficiency") @set:PropertyName("efficiency") var productionEfficiencyGain: Double = 0.0,
    @get:PropertyName("fertility") @set:PropertyName("fertility") var fertilityRate: Double = 0.0
)
