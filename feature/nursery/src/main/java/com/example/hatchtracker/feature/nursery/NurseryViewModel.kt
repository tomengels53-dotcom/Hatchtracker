package com.example.hatchtracker.feature.nursery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Flocklet
import com.example.hatchtracker.data.repository.NurseryRepository
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.data.models.FinancialSummary
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.example.hatchtracker.data.service.BirdLifecycleService
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.core.domain.models.StandardListUiState
import com.example.hatchtracker.core.domain.models.ListSection
import com.example.hatchtracker.core.domain.models.groupByStandardSection
import com.example.hatchtracker.feature.nursery.models.NurseryRowModel
import com.example.hatchtracker.core.common.HatchyGuidanceEngine
import com.example.hatchtracker.core.common.asString
import com.example.hatchtracker.model.Species
import java.util.Locale

@dagger.hilt.android.lifecycle.HiltViewModel
class NurseryViewModel @javax.inject.Inject constructor(
    private val repository: NurseryRepository,
    private val financialRepository: FinancialRepository,
    private val flockRepository: com.example.hatchtracker.data.repository.FlockRepository,
    private val lifecycleService: BirdLifecycleService,
    private val subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager,
    private val breedStandardRepository: com.example.hatchtracker.data.repository.BreedStandardRepository,
    private val incubationRepository: IncubationRepository,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    val currentCapabilities = subscriptionStateManager.currentCapabilities
    val flocks: StateFlow<List<com.example.hatchtracker.data.models.Flock>> = flockRepository.allActiveFlocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencyCode: StateFlow<String> = userRepository.userProfile
        .map { it?.currencyCode ?: "USD" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val isAdmin: StateFlow<Boolean> = subscriptionStateManager.isAdmin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isDeveloper: StateFlow<Boolean> = subscriptionStateManager.isDeveloper
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    fun getBreedsForSpecies(species: String): List<com.example.hatchtracker.data.models.BreedStandard> {
        return breedStandardRepository.getBreedsForSpecies(species)
    }
    
    /**
     * Get all species with gating information based on current subscription tier.
     */
    fun getSpeciesUiRows(): List<com.example.hatchtracker.domain.util.SpeciesUiRow> {
        val caps = currentCapabilities.value
        val isAdminVal = subscriptionStateManager.isAdmin.value
        val isAdminOrDev = subscriptionStateManager.isAdmin.value || subscriptionStateManager.isDeveloper.value
        
        return com.example.hatchtracker.domain.util.SpeciesGatingHelper.getSpeciesUiRows(
            tier = caps.tier,
            isAdminOrDeveloper = isAdminOrDev
        )
    }
    
    /**
     * Check if a species can be used with the current subscription tier.
     */
    fun canUseSpecies(speciesId: String): Boolean {
        val caps = currentCapabilities.value
        val isAdminVal = subscriptionStateManager.isAdmin.value
        val isAdminOrDev = subscriptionStateManager.isAdmin.value || subscriptionStateManager.isDeveloper.value
        
        return com.example.hatchtracker.domain.util.SpeciesGatingHelper.canUseSpecies(
            speciesId = speciesId,
            tier = caps.tier,
            isAdminOrDeveloper = isAdminOrDev
        )
    }

    @androidx.compose.runtime.Immutable
    data class ManualFlockletState(
        val species: com.example.hatchtracker.data.models.Species? = null,
        val breedDetails: List<com.example.hatchtracker.core.navigation.BreedSelectionResult> = emptyList(),
        val chickCount: String = "",
        val ageDays: String = "0"
    )

    private val _manualFlockletState = kotlinx.coroutines.flow.MutableStateFlow(ManualFlockletState())
    val manualFlockletState = _manualFlockletState.asStateFlow()

    fun updateManualFlockletState(
        species: com.example.hatchtracker.data.models.Species? = _manualFlockletState.value.species,
        chickCount: String = _manualFlockletState.value.chickCount,
        ageDays: String = _manualFlockletState.value.ageDays
    ) {
        val current = _manualFlockletState.value
        val breeds = if (species?.id != current.species?.id) emptyList() else current.breedDetails
        _manualFlockletState.value = current.copy(
            species = species,
            breedDetails = breeds,
            chickCount = chickCount,
            ageDays = ageDays
        )
    }

    fun onBreedSelected(result: com.example.hatchtracker.core.navigation.BreedSelectionResult) {
        val current = _manualFlockletState.value
        if (current.species?.id != result.speciesId) return

        if (current.breedDetails.none { it.breedId == result.breedId }) {
            val max = currentCapabilities.value.maxBreedsPerBatch
            if (current.breedDetails.size < max) {
                _manualFlockletState.value = current.copy(breedDetails = current.breedDetails + result)
            }
        }
    }

    fun removeBreed(breedId: String) {
        val current = _manualFlockletState.value
        _manualFlockletState.value = current.copy(breedDetails = current.breedDetails.filter { it.breedId != breedId })
    }

    fun clearManualFlockletState() {
        _manualFlockletState.value = ManualFlockletState()
    }
    private val _filter = kotlinx.coroutines.flow.MutableStateFlow("All")
    private val _sortMode = kotlinx.coroutines.flow.MutableStateFlow("Urgency")
    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    private val _collapsedSections = kotlinx.coroutines.flow.MutableStateFlow(setOf(ListSection.ARCHIVED))

    fun updateFilter(filter: String) { _filter.value = filter }
    fun updateSortMode(sort: String) { _sortMode.value = sort }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun toggleSection(section: ListSection) {
        val current = _collapsedSections.value.toMutableSet()
        if (current.contains(section)) current.remove(section) else current.add(section)
        _collapsedSections.value = current
    }

    val uiState: StateFlow<StandardListUiState<NurseryRowModel>> = combine(
        repository.activeFlocklets,
        _filter,
        _sortMode,
        _searchQuery,
        _collapsedSections
    ) { list, filter, sort, query, collapsed ->
        
        com.example.hatchtracker.core.common.PerformanceTrace.markStart()
        
        // 1. Map to DTOs and handle daily state calculations on the fly
        val dtos = list.mapNotNull { flocklet ->
            // Minimal domain logic
            val state = repository.calculateDailyState(flocklet)
            
            // Handle filters
            if (filter == "Ready" && !state.readyForFlock) return@mapNotNull null
            
            // Handle search
            if (query.isNotBlank() && !flocklet.species.contains(query, ignoreCase = true) && !flocklet.breeds.joinToString().contains(query, ignoreCase = true)) {
                return@mapNotNull null
            }
            
            val urgencyScore = when {
                state.readyForFlock -> 100
                flocklet.healthStatus.lowercase() != "healthy" -> 90
                else -> flocklet.ageInDays * 2 // simple proxy for demo
            }

            NurseryRowModel(
                id = flocklet.id.toString(),
                title = "${flocklet.species} Batch",
                subtitle = "${flocklet.chickCount} chicks \u2022 Age: ${state.ageInDays}d",
                statusText = if (state.readyForFlock) "Ready" else flocklet.healthStatus,
                urgencyScore = urgencyScore,
                isCompletedOrArchived = false,
                dueToday = state.readyForFlock, // rough heuristic
                dueWithin7Days = state.ageInDays > 21,
                speciesName = flocklet.species,
                chickCount = flocklet.chickCount,
                originalFlocklet = flocklet
            )
        }
        
        // 2. Sort
        val sorted = when (sort) {
            "Oldest" -> dtos.sortedBy { it.originalFlocklet.ageInDays }
            "Newest" -> dtos.sortedByDescending { it.originalFlocklet.ageInDays }
            else -> dtos.sortedByDescending { it.urgencyScore } // "Urgency" is default
        }
        
        // 3. Group
        val groupedItems = groupByStandardSection(sorted)

        StandardListUiState(
            items = sorted,
            groupedItems = groupedItems,
            filter = filter,
            sortMode = sort,
            searchQuery = query,
            collapsedSections = collapsed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StandardListUiState())

    /**
     * batch updates all flocklets.
     * Can be called on app start or via WorkManager.
     */
    fun triggerDailyUpdates() {
        viewModelScope.launch {
            repository.performDailyUpdates()
        }
    }

    /**
     * Manually triggers daily temperature reduction for a flocklet.
     * (Logic handled by Repository.calculateDailyState, this ensures it's persisted)
     */
    fun reduceTemperatureDaily(flocklet: Flocklet) {
        viewModelScope.launch {
            val updated = repository.calculateDailyState(flocklet)
            repository.updateFlocklet(updated)
        }
    }

    /**
     * Checks if flocklet is ready for flock.
     * (Logic contained in Repository.calculateDailyState, here we explicitly verify)
     */
    fun calculateReadiness(flocklet: Flocklet): Boolean {
        // We reuse the central logic from repo
        val state = repository.calculateDailyState(flocklet)
        return state.readyForFlock
    }

    /**
     * Moves a ready flocklet to a flock (archives it from nursery).
     * Now expands the flocklet into individual birds.
     * If targetFlockId is null, creates an Auto-Flock.
     */
    fun graduateFlocklet(flocklet: Flocklet, targetFlockId: Long?) {
        if (!flocklet.readyForFlock) return // Safety check

        viewModelScope.launch {
            // Use new batch method for single item
            lifecycleService.graduateFlockletsToFlock(listOf(flocklet.id), targetFlockId)
        }
    }

    /**
     * Legacy support / Explicit New Flock creation
     * Creates a new flock (with specific name/purpose) and moves the flocklet.
     */
    fun createNewFlockAndMoveFlocklet(flocklet: Flocklet, flockName: String, purpose: String) {
        if (!flocklet.readyForFlock) return

        viewModelScope.launch {
            val newFlock = com.example.hatchtracker.data.models.Flock(
                syncId = java.util.UUID.randomUUID().toString(),
                name = flockName,
                species = parseSpecies(flocklet.species),
                breeds = flocklet.breeds,
                purpose = purpose
            )
            // Use the explicit service method for "into New Flock"
            lifecycleService.graduateFlockletIntoNewFlock(flocklet.id, newFlock)
        }
    }

    /**
     * Updates manual status like health and notes.
     */
    fun updateFlockletStatus(flocklet: Flocklet, health: String, notes: String) {
        viewModelScope.launch {
            val updated = flocklet.copy(
                healthStatus = health,
                notes = notes,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateFlocklet(updated)
        }
    }

    fun addManualFlocklet(flocklet: Flocklet) {
        viewModelScope.launch {
            val created = lifecycleService.addManualFlocklet(flocklet)
            val caps = subscriptionStateManager.currentCapabilities.value
            val isAdmin = subscriptionStateManager.isAdmin.value
            val isDeveloper = subscriptionStateManager.isDeveloper.value
            val canScheduleNursery = FeatureAccessPolicy
                .canAccess(FeatureKey.NURSERY, caps.tier, isAdmin || isDeveloper)
                .allowed
            NotificationHelper.scheduleNurseryMilestones(
                context = appContext,
                flocklet = created,
                canSchedule = canScheduleNursery
            )
        }
    }

    fun submitManualFlocklet() {
        val state = _manualFlockletState.value
        val species = state.species ?: return
        if (state.chickCount.isBlank()) return
        
        val count = state.chickCount.toIntOrNull() ?: return
        val age = state.ageDays.toIntOrNull() ?: 0
        val hatchDate = System.currentTimeMillis() - (age * 86400000L)
        val breedNames = state.breedDetails.map { it.breedName }.ifEmpty { listOf("Mixed") }
        
        val newFlocklet = Flocklet(
            id = 0,
            hatchId = null,
            species = species.name,
            breeds = breedNames,
            hatchDate = hatchDate,
            chickCount = count,
            currentTemp = 0.0,
            targetTemp = 0.0,
            ageInDays = age,
            weightAvg = 0.0,
            healthStatus = "Healthy",
            notes = null,
            readyForFlock = false,
            movedToFlockId = null,
            syncId = java.util.UUID.randomUUID().toString(),
            lastUpdated = System.currentTimeMillis()
        )
        
        addManualFlocklet(newFlocklet)
        clearManualFlockletState()
    }

    /**
     * Helper to observe summary for a specific flocklet.
     */
    fun observeSummary(syncId: String): StateFlow<FinancialSummary?> {
        return financialRepository.observeFinancialSummary(syncId, "flocklet")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    val hatchyAdvice: StateFlow<Map<Long, String>> = repository.activeFlocklets.map { list ->
        val adviceMap: Map<Long, String> = list.associate { flocklet ->
            // Temperature is now just a target to check
            val advice = HatchyGuidanceEngine.getNurseryAdvice(
                flocklet.ageInDays,
                flocklet.currentTemp,
                flocklet.targetTemp
            ).asString(appContext)
            flocklet.id to advice
        }
        adviceMap
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun parseSpecies(value: String): Species {
        return runCatching { Species.valueOf(value.trim().uppercase(Locale.US)) }
            .getOrDefault(Species.UNKNOWN)
    }

    fun sellFlocklet(flocklet: Flocklet, quantity: Int, price: Double, date: Long, buyerName: String, notes: String) {
        viewModelScope.launch {
            lifecycleService.markSold(
                sourceType = com.example.hatchtracker.data.models.BirdLifecycleStage.FLOCKLET,
                sourceId = flocklet.id,
                syncId = flocklet.syncId,
                quantity = quantity,
                price = price,
                date = date,
                notes = notes,
                buyerName = buyerName.ifBlank { null }
            )
        }
    }

    val hasCompletedIncubations: StateFlow<Boolean> = incubationRepository.allIncubations
        .map { incubations ->
            incubations.any { it.hatchCompleted && it.hatchedCount > 0 && it.lifecycleStage == com.example.hatchtracker.data.models.BirdLifecycleStage.FLOCKLET }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun removeFlocklet(flocklet: Flocklet, reason: String) {
        viewModelScope.launch {
            lifecycleService.removeFlocklet(flocklet.id, reason)
        }
    }

    fun recordLoss(flocklet: Flocklet, count: Int, reason: String) {
        viewModelScope.launch {
            lifecycleService.recordFlockletDeath(flocklet.id, count, reason)
        }
    }
}
