package com.example.hatchtracker.feature.flock.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.model.BreedStandard
import com.example.hatchtracker.data.models.FinancialSummary
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.data.repository.BreedStandardRepository
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.service.BirdLifecycleService
import com.example.hatchtracker.billing.SubscriptionStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.hatchtracker.core.domain.models.StandardListUiState
import com.example.hatchtracker.core.domain.models.ListSection
import com.example.hatchtracker.core.domain.models.groupByStandardSection
import com.example.hatchtracker.feature.flock.models.FlockRowModel

@HiltViewModel
class FlockViewModel @Inject constructor(
    private val repository: FlockRepository,
    private val subscriptionManager: SubscriptionStateManager,
    private val breedStandardRepository: BreedStandardRepository,
    private val financialRepository: FinancialRepository,
    private val lifecycleService: BirdLifecycleService,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    private val profileImageRepository: com.example.hatchtracker.core.data.repository.ProfileImageRepository,
    val formatService: com.example.hatchtracker.common.format.LocaleFormatService
) : ViewModel() {

    val currentCapabilities = subscriptionManager.currentCapabilities

    val dateFormat: StateFlow<String> = userRepository.userProfile
        .map { it?.dateFormat ?: "DD-MM-YYYY" }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DD-MM-YYYY")

    val currencyCode: StateFlow<String> = userRepository.userProfile
        .map { it?.currencyCode ?: "USD" }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")
    
    
    fun getBreedsForSpecies(species: String): List<BreedStandard> {
        return breedStandardRepository.getBreedsForSpecies(species)
    }
    
    /**
     * Reactive list of species with gating information.
     */
    val speciesUiRows: StateFlow<List<com.example.hatchtracker.domain.util.SpeciesUiRow>> = 
        kotlinx.coroutines.flow.combine(
            currentCapabilities,
            userRepository.userProfile
        ) { caps, profile ->
            val isAdminOrDev = profile?.isDeveloper == true || profile?.isSystemAdmin == true
            com.example.hatchtracker.domain.util.SpeciesGatingHelper.getSpeciesUiRows(
                tier = caps.tier,
                isAdminOrDeveloper = isAdminOrDev
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * Check if a species can be used with the current subscription tier.
     */
    fun canUseSpecies(speciesId: String): Boolean {
        val caps = currentCapabilities.value
        val profile = userRepository.userProfile.value
        val isAdminOrDev = profile?.isDeveloper == true || profile?.isSystemAdmin == true
        
        return com.example.hatchtracker.domain.util.SpeciesGatingHelper.canUseSpecies(
            speciesId = speciesId,
            tier = caps.tier,
            isAdminOrDeveloper = isAdminOrDev
        )
    }

    /**
     * Check if more flocks can be created based on current count and tier.
     */
    fun canCreateMoreFlocks(currentCount: Int): Boolean {
        return currentCount < currentCapabilities.value.maxFlocks
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

    val uiState: StateFlow<StandardListUiState<FlockRowModel>> = combine(
        repository.allActiveFlocks,
        _filter,
        _sortMode,
        _searchQuery,
        _collapsedSections
    ) { list, filter, sort, query, collapsed ->
        com.example.hatchtracker.core.common.PerformanceTrace.markStart()

        val dtos = list.mapNotNull { flock ->
            // Apply filtering
            if (filter == "Active" && !flock.active) return@mapNotNull null
            if (filter == "Archived" && flock.active) return@mapNotNull null
            
            // Search
            if (query.isNotBlank() && !flock.name.contains(query, ignoreCase = true) && !flock.species.name.contains(query, ignoreCase = true)) {
                return@mapNotNull null
            }

            // Health proxy logic - could be expanded if Flock had health lists
            val needsAttention = false // proxy
            
            val urgencyScore = when {
                !flock.active -> 0
                needsAttention -> 100
                else -> 50
            }

            FlockRowModel(
                id = flock.id.toString(),
                title = flock.name.ifBlank { "Unnamed Flock" },
                subtitle = "${flock.species} \u2022 ${flock.purpose}",
                statusText = if (flock.active) "Active" else "Inactive",
                urgencyScore = urgencyScore,
                isCompletedOrArchived = !flock.active,
                dueToday = needsAttention,
                dueWithin7Days = false,
                birdCount = 0,
                isArchived = !flock.active,
                originalFlock = flock
            )
        }

        val sorted = when (sort) {
            "A-Z" -> dtos.sortedBy { it.title }
            "Species" -> dtos.sortedBy { it.originalFlock.species }
            else -> dtos.sortedByDescending { it.urgencyScore }
        }

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

    // Keep allFlocks around for anything else
    val allFlocks: StateFlow<List<Flock>> = repository.allActiveFlocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFlock(flock: Flock) {
        viewModelScope.launch {
            repository.insertFlock(flock)
        }
    }

    suspend fun saveProfilePhoto(uri: android.net.Uri): Result<String?> {
        return profileImageRepository.saveProfilePhoto(uri)
    }

    fun deleteOldPhoto(path: String?) {
        profileImageRepository.deleteOldPhoto(path)
    }

    /**
     * Creates a new flock and immediately graduates a flocklet into it.
     */
    fun addFlockAndGraduate(flock: Flock, flockletId: Long) {
        viewModelScope.launch {
            lifecycleService.graduateFlockletIntoNewFlock(flockletId, flock)
        }
    }

    fun deleteFlock(flock: Flock, reason: String = "Manual deletion") {
        viewModelScope.launch {
            profileImageRepository.deleteOldPhoto(flock.imagePath)
            lifecycleService.removeFlock(flock, reason)
        }
    }

    fun updateFlock(flock: Flock, name: String, notes: String, purpose: String, active: Boolean, breeds: List<String>) {
        viewModelScope.launch {
            repository.updateFlock(flock.copy(
                name = name,
                notes = notes.ifBlank { null },
                purpose = purpose,
                active = active,
                breeds = breeds
            ))
        }
    }

    fun sellFlock(flock: Flock, quantity: Int, price: Double, date: Long, buyerName: String, notes: String) {
        viewModelScope.launch {
            lifecycleService.markSold(
                sourceType = com.example.hatchtracker.data.models.BirdLifecycleStage.ADULT,
                sourceId = flock.id,
                syncId = flock.syncId,
                quantity = quantity,
                price = price,
                date = date,
                buyerName = buyerName.ifBlank { null },
                notes = notes
            )
        }
    }

    fun observeSummary(ownerId: String): StateFlow<FinancialSummary?> {
        return financialRepository.observeFinancialSummary(ownerId, "flock")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }
}
