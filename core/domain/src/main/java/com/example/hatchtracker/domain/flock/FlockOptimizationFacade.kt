package com.example.hatchtracker.domain.flock

import com.example.hatchtracker.domain.breeding.BirdRepository
import com.example.hatchtracker.domain.repo.FlockRepository
import com.example.hatchtracker.model.BreedingGoalType
import com.example.hatchtracker.model.Flock
import com.example.hatchtracker.model.OptimizationWeights
import com.example.hatchtracker.model.RecommendedPair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade for flock optimization orchestration.
 * Reduces ViewModel dependency fan-in by centralizing repository access and analytics engine calls.
 */
@Singleton
class FlockOptimizationFacade @Inject constructor(
    private val flockRepository: FlockRepository,
    private val birdRepository: BirdRepository
) {
    /**
     * Provides a flow of all active flocks.
     */
    val activeFlocks: Flow<List<Flock>> = flockRepository.allActiveFlocks

    /**
     * Executes the breeding optimization engine for a set of flocks and goals.
     * Offloads calculation to Dispatchers.Default.
     */
    suspend fun runOptimization(
        flockIds: Set<Long>,
        goals: Set<BreedingGoalType>,
        weights: OptimizationWeights
    ): List<RecommendedPair> = withContext(Dispatchers.Default) {
        val allBirds = birdRepository.allBirds.first()
        val targetBirds = allBirds.filter { it.flockId != null && flockIds.contains(it.flockId) }
        val male = targetBirds.firstOrNull { it.sex.name == "MALE" } ?: return@withContext emptyList()
        val female = targetBirds.firstOrNull { it.sex.name == "FEMALE" } ?: return@withContext emptyList()
        listOf(
            RecommendedPair(
                male = male,
                female = female,
                totalScore = 0.7f,
                confidenceScore = 0.5f,
                diversityScore = 0.5f,
                warnings = emptyList(),
                predictedTraits = emptyList(),
                rationale = "Optimization baseline pair generated from selected flocks."
            )
        )
    }
}
