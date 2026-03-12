package com.example.hatchtracker.domain.nursery

import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.data.repository.*
import com.example.hatchtracker.data.service.BirdLifecycleService
import com.example.hatchtracker.data.models.FinancialSummary
import com.example.hatchtracker.domain.subscription.AppCapabilities
import com.example.hatchtracker.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade for nursery orchestration.
 * Centralizes access to nursery data, financial summaries, and lifecycle transitions.
 * Handles feature gating and notification scheduling for manual entries.
 */
@Singleton
class NurseryFacade @Inject constructor(
    private val nurseryRepository: NurseryRepository,
    private val financialRepository: FinancialRepository,
    private val flockRepository: FlockRepository,
    private val lifecycleService: BirdLifecycleService,
    private val breedStandardRepository: BreedStandardRepository,
    private val incubationRepository: IncubationRepository,
    private val userRepository: UserRepository
) {
    /**
     * Observable stream of active flocklets.
     */
    val activeFlocklets: Flow<List<Flocklet>> = nurseryRepository.activeFlocklets

    /**
     * Observable stream of all active flocks.
     */
    val activeFlocks: Flow<List<Flock>> = flockRepository.allActiveFlocks

    /**
     * Current subscription capabilities.
     */
    val currentCapabilities = MutableStateFlow(AppCapabilities())

    /**
     * Admin/Developer status flows.
     */
    val isAdmin: Flow<Boolean> = flowOf(false)
    val isDeveloper: Flow<Boolean> = flowOf(false)

    /**
     * User's preferred currency code.
     */
    val currencyCode: Flow<String> = userRepository.userProfile.map { it?.currencyCode ?: "USD" }

    /**
     * Stream of advice mapping for active flocklets.
     */
    val hatchyAdvice: Flow<Map<Long, UiText>> = activeFlocklets.map { list ->
        list.associate { flocklet ->
            val advice = com.example.hatchtracker.core.common.HatchyGuidanceEngine.getNurseryAdvice(
                flocklet.ageInDays,
                flocklet.currentTemp,
                flocklet.targetTemp
            )
            flocklet.id to advice
        }
    }

    /**
     * Checks if any completed incubations are currently in the nursery stage.
     */
    val hasCompletedIncubations: Flow<Boolean> = incubationRepository.allIncubations.map { list ->
        list.any { it.hatchCompleted && it.hatchedCount > 0 && it.lifecycleStage == BirdLifecycleStage.FLOCKLET }
    }

    /**
     * Returns the calculated state (age, readiness) for a flocklet.
     */
    fun calculateDailyState(flocklet: Flocklet) = nurseryRepository.calculateDailyState(flocklet)

    /**
     * Graduates a flocklet to an existing or auto-created flock.
     */
    suspend fun graduateFlockletToFlock(flockletId: Long, targetFlockId: Long?) {
        lifecycleService.graduateFlockletsToFlock(listOf(flockletId), targetFlockId)
    }

    /**
     * Creates a new flock and graduates a flocklet into it.
     */
    suspend fun createNewFlockAndGraduate(flocklet: Flocklet, flockName: String, purpose: String) {
        val species = try {
            Species.valueOf(flocklet.species.uppercase())
        } catch (_: IllegalArgumentException) {
            Species.UNKNOWN
        }
        val newFlock = Flock(
            syncId = java.util.UUID.randomUUID().toString(),
            name = flockName,
            species = species,
            breeds = flocklet.breeds,
            purpose = purpose
        )
        lifecycleService.graduateFlockletIntoNewFlock(flocklet.id, newFlock)
    }

    /**
     * Adds a flocklet manually and schedules associated milestones.
     */
    suspend fun addManualFlocklet(flocklet: Flocklet) {
        lifecycleService.addManualFlocklet(flocklet)
        val caps = currentCapabilities.value
        val isAdminOrDev = false

        FeatureAccessPolicy.canAccess(FeatureKey.NURSERY, caps.tier, isAdminOrDev).allowed
    }

    /**
     * Observes the financial summary for a flocklet.
     */
    fun observeFinancialSummary(syncId: String): Flow<FinancialSummary?> {
        return financialRepository.observeFinancialSummary(syncId, "flocklet")
    }

    /**
     * Batch updates all flocklets for daily aging.
     */
    suspend fun triggerDailyUpdates() = nurseryRepository.performDailyUpdates()

    /**
     * Standard update for a flocklet's metadata or state.
     */
    suspend fun updateFlocklet(flocklet: Flocklet) = nurseryRepository.updateFlocklet(flocklet)
    
    /**
     * Marks a flocklet as sold with financial attribution.
     */
    suspend fun markSold(flockletId: Long, syncId: String, quantity: Int, price: Double, date: Long, buyerName: String, notes: String) {
        lifecycleService.markSold(
            sourceType = BirdLifecycleStage.FLOCKLET,
            sourceId = flockletId,
            syncId = syncId,
            quantity = quantity,
            price = price,
            date = date,
            notes = notes,
            buyerName = buyerName.ifBlank { null }
        )
    }

    /**
     * Returns the list of standard breeds for a specific species.
     */
    fun getBreedsForSpecies(species: String) = breedStandardRepository.getBreedsForSpecies(species)
    
    /**
     * Removes a flocklet from the system with a specific reason.
     */
    suspend fun removeFlocklet(flockletId: Long, reason: String) = lifecycleService.removeFlocklet(flockletId, reason)

    /**
     * Records a mortality event for a flocklet.
     */
    suspend fun recordLoss(flockletId: Long, count: Int, reason: String) = lifecycleService.recordFlockletDeath(flockletId, count, reason)
}
