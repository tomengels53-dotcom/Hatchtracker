package com.example.hatchtracker.domain.flock

import com.example.hatchtracker.domain.breeding.BirdRepository
import com.example.hatchtracker.domain.repo.FlockRepository
import com.example.hatchtracker.domain.subscription.AppCapabilities
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.EggProduction
import com.example.hatchtracker.model.FinancialSummary
import com.example.hatchtracker.model.Flock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlockDetailFacade @Inject constructor(
    private val flockRepository: FlockRepository,
    private val birdRepository: BirdRepository
) {
    val currencyCode: Flow<String> = flowOf("USD")
    val dateFormat: Flow<String> = flowOf("DD-MM-YYYY")
    val capabilities = MutableStateFlow(AppCapabilities())
    val activeFlocks: Flow<List<Flock>> = flockRepository.allActiveFlocks

    fun getFlock(flockId: Long): Flow<Flock?> = flockRepository.getFlockFlow(flockId)

    fun getBirdsForFlock(flockId: Long): Flow<List<Bird>> =
        birdRepository.allBirds.map { all -> all.filter { it.flockId == flockId } }

    fun observeFinancialSummary(flockSyncId: String): Flow<FinancialSummary?> = flowOf(null)

    fun observeEggProduction(flockSyncId: String): Flow<List<EggProduction>> = flowOf(emptyList())

    suspend fun moveBirds(birdIds: List<Long>, fromFlockId: Long, toFlockId: Long) = Unit

    suspend fun updateFlock(flock: Flock) {
        flockRepository.updateFlock(flock)
    }

    suspend fun removeFlock(flock: Flock, reason: String) {
        flockRepository.deleteFlock(flock)
    }

    suspend fun removeBirds(birdIds: List<Long>, reason: String) = Unit
}
