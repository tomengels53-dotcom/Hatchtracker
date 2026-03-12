package com.example.hatchtracker.feature.incubation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.repository.IncubationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.data.models.FinancialSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.hatchtracker.data.service.BirdLifecycleService

@HiltViewModel
class IncubationDetailViewModel @Inject constructor(
    private val repository: IncubationRepository,
    private val deviceRepository: com.example.hatchtracker.data.repository.DeviceRepository,
    private val deviceCapacityManager: com.example.hatchtracker.domain.breeding.DeviceCapacityManager,
    savedStateHandle: SavedStateHandle,
    private val subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager,
    private val financialRepository: FinancialRepository,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    private val lifecycleService: BirdLifecycleService,
    private val breedingAnalyzer: com.example.hatchtracker.core.common.BreedingAnalyzer,
    val localeFormatService: com.example.hatchtracker.common.format.LocaleFormatService
) : ViewModel() {

    val currencyCode: StateFlow<String> = userRepository.userProfile
        .map { it?.currencyCode ?: "USD" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val dateFormat: StateFlow<String> = userRepository.userProfile
        .map { it?.dateFormat ?: "DD-MM-YYYY" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DD-MM-YYYY")

    val timeFormat: StateFlow<String> = userRepository.userProfile
        .map { it?.timeFormat ?: "24h" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "24h")

    private val incubationId: Long = checkNotNull(savedStateHandle["incubationId"])

    val incubation = kotlinx.coroutines.flow.flow { 
        emit(repository.getIncubationById(incubationId)) 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val unitCostResult = incubation.map { inc ->
        if (inc == null) null
        else com.example.hatchtracker.domain.pricing.unitcost.UnitCostResult.Unavailable(emptySet(), "Calculating...")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val fertilityRate: StateFlow<Float> = incubation.map { inc ->
        if (inc == null) 0f
        else com.example.hatchtracker.domain.breeding.IncubationUtils.calculateFertilityRate(inc.eggsCount, inc.infertileCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val hatchability: StateFlow<Float> = incubation.map { inc ->
        if (inc == null) 0f
        else com.example.hatchtracker.domain.breeding.IncubationUtils.calculateHatchability(inc.hatchedCount, inc.eggsCount, inc.infertileCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    data class PerformanceState(
        val fertilityRate: Float,
        val hatchability: Float,
        val costPerHatch: Double? = null,
        val isCompleted: Boolean = false
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val performanceState: StateFlow<PerformanceState?> = incubation.flatMapLatest { inc ->
        if (inc == null) flowOf(null)
        else {
            financialInsights.map { insights ->
                PerformanceState(
                    fertilityRate = com.example.hatchtracker.domain.breeding.IncubationUtils.calculateFertilityRate(inc.eggsCount, inc.infertileCount),
                    hatchability = com.example.hatchtracker.domain.breeding.IncubationUtils.calculateHatchability(inc.hatchedCount, inc.eggsCount, inc.infertileCount),
                    costPerHatch = insights?.costPerHatch,
                    isCompleted = inc.hatchCompleted
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val financialSummary: StateFlow<com.example.hatchtracker.data.models.FinancialStats?> = incubation
        .flatMapLatest { incubation ->
            if (incubation != null) {
                financialRepository.getAggregatedStats(
                    ownerType = "incubation",
                    ownerId = incubation.id.toString(),
                    startDate = 0L,
                    endDate = System.currentTimeMillis()
                )
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val financialInsights: StateFlow<com.example.hatchtracker.core.common.FinancialInsights?> = incubation
        .flatMapLatest { incubation ->
            if (incubation != null) {
                financialSummary.map { stats ->
                    if (stats != null) {
                        val breederScore = breedingAnalyzer.calculateBreederScore(
                            listOf(incubation),
                            emptyList() // We don't have offspring list here easily
                        )
                        breedingAnalyzer.calculateFinancialInsights(
                            incubation = incubation,
                            totalCosts = stats.totalCost,
                            breederScore = breederScore
                        )
                    } else null
                }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val capabilities = subscriptionStateManager.currentCapabilities

    val hatchyAdvice: StateFlow<String?> = incubation.map {
        if (it == null) null
        else {
             val daysRemaining = com.example.hatchtracker.domain.breeding.IncubationUtils.calculateDaysUntilHatch(it.expectedHatch)
             val progress = com.example.hatchtracker.domain.breeding.IncubationUtils.calculateIncubationProgress(it.startDate, it.expectedHatch)
             
             when {
                 daysRemaining <= 3 -> "hatchy_advice_lockdown"
                 daysRemaining <= 7 -> "hatchy_advice_approaching"
                 progress > 0.5 -> "hatchy_advice_mid_incubation"
                 else -> "hatchy_advice_early"
             }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // Load hatchers with capacity
    val userHatchers = deviceCapacityManager.getCapacityForDevices(
        deviceRepository.getUserDevices().map { devices ->
            devices.filter { it.type == com.example.hatchtracker.model.DeviceType.HATCHER }
        }
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expose all devices for name resolution
    val allDevices = deviceRepository.getUserDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun assignHatcher(hatcherId: String?, keepInIncubator: Boolean) {
        val current = incubation.value ?: return
        
        viewModelScope.launch {
            val updated = current.copy(
                // If moving to hatcher: set hatcher ID. If manual/none: null.
                hatcherDeviceId = hatcherId ?: "",
                // If moving to hatcher or manual (not keeping): clear incubator. 
                // If keeping: retain incubator ID.
                incubatorDeviceId = if (keepInIncubator) current.incubatorDeviceId else ""
            )
            repository.update(updated)
            // Force refresh? Flow should handle it if room observes.
        }
    }

    fun updateIncubation(startDate: String, eggsCount: Int, notes: String, deviceId: String) {
        val current = incubation.value ?: return
        
        // Validation: eggsCount >= (hatched + infertile + failed)
        val minimumEggs = current.hatchedCount + current.infertileCount + current.failedCount
        if (eggsCount < minimumEggs) return

        viewModelScope.launch {
            repository.update(current.copy(
                startDate = startDate,
                eggsCount = eggsCount,
                notes = notes.ifBlank { null },
                incubatorDeviceId = deviceId
            ))
        }
    }

    fun deleteIncubation() {
        val current = incubation.value ?: return
        viewModelScope.launch {
            repository.deleteIncubation(current)
        }
    }

    fun sellEggs(quantity: Int, price: Double, date: Long, buyerName: String, notes: String) {
        val current = incubation.value ?: return
        viewModelScope.launch {
            lifecycleService.markSold(
                sourceType = com.example.hatchtracker.data.models.BirdLifecycleStage.INCUBATING,
                sourceId = current.id,
                syncId = current.syncId,
                quantity = quantity,
                price = price,
                date = date,
                buyerName = buyerName.ifBlank { null },
                notes = notes
            )
        }
    }
}






