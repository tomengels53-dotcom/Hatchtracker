package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.CollaborativeProjectDto
import com.example.hatchtracker.data.models.ProjectMetricsDto
import com.example.hatchtracker.data.models.ProjectParticipantDto
import com.example.hatchtracker.domain.model.CollaborativeBreedingProject
import com.example.hatchtracker.domain.model.ProjectMetrics
import com.example.hatchtracker.domain.model.ProjectParticipant
import com.example.hatchtracker.domain.model.ParticipantRole
import com.example.hatchtracker.domain.model.ProjectStatus
import com.example.hatchtracker.model.Species
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollaborativeBreedingProjectRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val projectsCollection = firestore.collection("community_projects")

    suspend fun getProject(projectId: String): CollaborativeBreedingProject? {
        val doc = projectsCollection.document(projectId).get().await()
        return doc.toObject(CollaborativeProjectDto::class.java)?.toDomain()
    }

    suspend fun joinProject(projectId: String, userId: String, username: String) {
        val project = getProject(projectId) ?: return
        val updatedParticipants = project.participants.toMutableList()
        if (updatedParticipants.none { it.userId == userId }) {
            updatedParticipants.add(ProjectParticipant(userId, username))
            projectsCollection.document(projectId).update("participants", updatedParticipants.map { it.toDto() }).await()
        }
    }

    suspend fun updateMetrics(projectId: String, metrics: ProjectMetrics) {
        projectsCollection.document(projectId).update("metrics", metrics.toDto()).await()
    }
}

// Mappers
fun CollaborativeProjectDto.toDomain() = CollaborativeBreedingProject(
    projectId = projectId,
    species = try { Species.valueOf(species) } catch (e: Exception) { Species.UNKNOWN },
    breedFocus = breedFocus,
    title = title,
    goal = goal,
    generationTrackingEnabled = generationTrackingEnabled,
    participants = participants.map { it.toDomain() },
    aggregatedMetrics = metrics.toDomain(),
    createdAt = createdAt,
    status = try { ProjectStatus.valueOf(status) } catch (e: Exception) { ProjectStatus.OPEN }
)

fun ProjectParticipantDto.toDomain() = ProjectParticipant(
    userId = userId,
    username = username,
    role = try { ParticipantRole.valueOf(role) } catch (e: Exception) { ParticipantRole.CONTRIBUTOR },
    personalContributionCount = personalContributionCount,
    joinedAt = joinedAt
)

fun ProjectMetricsDto.toDomain() = ProjectMetrics(
    averageHatchRate = averageHatchRate,
    totalOffspringTracked = totalOffspringTracked,
    traitConsistencyScore = traitConsistencyScore,
    productionEfficiencyGain = productionEfficiencyGain,
    fertilityRate = fertilityRate
)

fun ProjectParticipant.toDto() = com.example.hatchtracker.data.models.ProjectParticipantDto(
    userId = userId,
    username = username,
    role = role.name,
    personalContributionCount = personalContributionCount,
    joinedAt = joinedAt
)

fun ProjectMetrics.toDto() = ProjectMetricsDto(
    averageHatchRate = averageHatchRate,
    totalOffspringTracked = totalOffspringTracked,
    traitConsistencyScore = traitConsistencyScore,
    productionEfficiencyGain = productionEfficiencyGain,
    fertilityRate = fertilityRate
)
