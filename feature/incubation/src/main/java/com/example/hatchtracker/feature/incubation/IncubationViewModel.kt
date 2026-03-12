package com.example.hatchtracker.feature.incubation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.repository.IncubationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.hatchtracker.data.service.BirdLifecycleService
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.hatchtracker.core.domain.models.StandardListUiState
import com.example.hatchtracker.core.domain.models.ListSection
import com.example.hatchtracker.core.domain.models.groupByStandardSection
import com.example.hatchtracker.feature.incubation.models.IncubationRowModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


@HiltViewModel
class IncubationViewModel @Inject constructor(
    private val repository: IncubationRepository,
    private val lifecycleService: BirdLifecycleService,
    private val featureAccess: com.example.hatchtracker.billing.FeatureAccess,
    private val subscriptionManager: com.example.hatchtracker.billing.SubscriptionStateManager,
    private val financialRepository: com.example.hatchtracker.data.repository.FinancialRepository,
    private val deviceRepository: com.example.hatchtracker.data.repository.DeviceRepository,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    private val breedingAnalyzer: com.example.hatchtracker.core.common.BreedingAnalyzer,
    val localeFormatService: com.example.hatchtracker.common.format.LocaleFormatService
) : ViewModel() {

    val currentCapabilities = subscriptionManager.currentCapabilities

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

    val hubSummary: StateFlow<com.example.hatchtracker.core.common.IncubationHubSummary> = repository.allIncubations
        .map { list ->
            val completed = list.filter { it.hatchCompleted }
                .sortedByDescending { it.startDate }
                .take(10) // Bounded window as requested
            
            breedingAnalyzer.calculateHubSummary(completed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.hatchtracker.core.common.IncubationHubSummary.Empty)

    val uiState: StateFlow<StandardListUiState<IncubationRowModel>> = combine(
        repository.allIncubations,
        _filter,
        _sortMode,
        _searchQuery,
        _collapsedSections
    ) { list, filter, sort, query, collapsed ->
        com.example.hatchtracker.core.common.PerformanceTrace.markStart()

        val dtos = list.mapNotNull { incubation ->
            // Minimal domain logic
            val isCompleted = incubation.hatchCompleted
            
            if (filter == "Active" && isCompleted) return@mapNotNull null
            if (filter == "Completed" && !isCompleted) return@mapNotNull null
            
            if (query.isNotBlank() && !incubation.species.contains(query, ignoreCase = true) && !incubation.breeds.joinToString().contains(query, ignoreCase = true)) {
                return@mapNotNull null
            }

            val dueTdy = isDueToday(incubation.expectedHatch)
            val due7 = isDueWithin7Days(incubation.expectedHatch)
            
            val urgencyScore = when {
                isCompleted -> 0
                dueTdy -> 100
                due7 -> 80
                else -> 50
            }

            val deviceName = allDevices.value.find { it.id == incubation.incubatorDeviceId }?.displayName

            IncubationRowModel(
                id = incubation.id.toString(),
                title = "${incubation.species} Batch",
                subtitle = "${incubation.eggsCount} eggs" + (if (deviceName != null) " \u2022 $deviceName" else ""),
                statusText = if (isCompleted) "Completed" else "Incubating",
                urgencyScore = urgencyScore,
                isCompletedOrArchived = isCompleted,
                dueToday = dueTdy,
                dueWithin7Days = due7,
                deviceName = deviceName,
                eggsCount = incubation.eggsCount,
                isCompleted = isCompleted,
                originalIncubation = incubation
            )
        }

        val sorted = when (sort) {
            "Oldest" -> dtos.sortedBy { it.originalIncubation.startDate }
            "Newest" -> dtos.sortedByDescending { it.originalIncubation.startDate }
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

    private fun isDueToday(isoDate: String): Boolean {
        val dateMillis = parseIsoDateUtcMillis(isoDate) ?: return false
        val diff = daysBetweenUtc(todayUtcMillis(), dateMillis)
        return diff == 0
    }

    private fun isDueWithin7Days(isoDate: String): Boolean {
        val dateMillis = parseIsoDateUtcMillis(isoDate) ?: return false
        val diff = daysBetweenUtc(todayUtcMillis(), dateMillis)
        return diff in 1..7
    }

    private val MILLIS_PER_DAY = 86_400_000L

    // Cached to avoid per-call allocation inside the combine flow (called on every incubation list update)
    private val isoDateParser = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private fun parseIsoDateUtcMillis(value: String): Long? {
        return runCatching {
            isoDateParser.get()!!.parse(value)?.time
        }.getOrNull()
    }

    private fun todayUtcMillis(): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun daysBetweenUtc(startMillis: Long, endMillis: Long): Int {
        return ((endMillis - startMillis) / MILLIS_PER_DAY).toInt()
    }

    // Keep allIncubations accessible if needed by other parts, but UI should use uiState
    val allIncubations: StateFlow<List<Incubation>> = repository.allIncubations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    val allDevices: StateFlow<List<com.example.hatchtracker.data.models.Device>> = deviceRepository.getUserDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dateFormat: StateFlow<String> = userRepository.userProfile
        .map { it?.dateFormat ?: "DD-MM-YYYY" }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DD-MM-YYYY")

    fun canCreateMoreIncubations(currentCount: Int): Boolean {
        return featureAccess.canCreateMoreThanNIncubations(currentCount)
    }

    fun deleteIncubation(incubation: Incubation, reason: String) {
        viewModelScope.launch {
            lifecycleService.removeIncubation(incubation.id, reason)
        }
    }

    fun sellEggs(incubation: Incubation, quantity: Int, price: Double, date: Long, buyerName: String, notes: String) {
        viewModelScope.launch {
            lifecycleService.markSold(
                sourceType = com.example.hatchtracker.data.models.BirdLifecycleStage.INCUBATING,
                sourceId = incubation.id,
                syncId = incubation.syncId,
                quantity = quantity,
                price = price,
                date = date,
                buyerName = buyerName.ifBlank { null },
                notes = notes
            )
        }
    }

    fun observeSummary(ownerId: String): StateFlow<com.example.hatchtracker.data.models.FinancialSummary?> {
        return financialRepository.observeFinancialSummary(ownerId, "incubation")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun updateIncubation(incubation: Incubation, startDate: String, eggsCount: Int, notes: String, deviceId: String) {
        val minimumEggs = incubation.hatchedCount + incubation.infertileCount + incubation.failedCount
        if (eggsCount < minimumEggs) return

        viewModelScope.launch {
            repository.update(incubation.copy(
                startDate = startDate,
                eggsCount = eggsCount,
                notes = notes.ifBlank { null },
                incubatorDeviceId = deviceId
            ))
        }
    }
}




