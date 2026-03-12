package com.example.hatchtracker.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.data.util.FinancialChartUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.domain.pricing.*
import com.example.hatchtracker.domain.breeding.MarketType
import com.example.hatchtracker.domain.pricing.unitcost.UnitCostResult
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.FinancialStats
import com.example.hatchtracker.model.EnrichedFinancialStats
import com.example.hatchtracker.model.FinancialTrustLevel

@HiltViewModel
class FinancialStatsViewModel @Inject constructor(
    private val repository: FinancialRepository,
    private val flockRepository: com.example.hatchtracker.data.repository.FlockRepository,
    private val incubationRepository: com.example.hatchtracker.data.repository.IncubationRepository,
    private val nurseryRepository: com.example.hatchtracker.data.repository.NurseryRepository,
    private val subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    private val unitCostProvider: UnitCostProvider,
    private val pricingSuggestionBuilder: PricingSuggestionBuilder
) : ViewModel() {

    val currencyCode: StateFlow<String> = userRepository.userProfile
        .map { it?.currencyCode ?: "USD" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    private val _ownerType = MutableStateFlow<String?>("flock")
    val ownerType = _ownerType.asStateFlow()

    private val _ownerId = MutableStateFlow<String?>(null)
    val ownerId = _ownerId.asStateFlow()

    private val _timeBucket = MutableStateFlow(FinancialChartUtil.TimeBucket.DAY)
    val timeBucket = _timeBucket.asStateFlow()

    private val _dateRange = MutableStateFlow(getDefaultDateRange())
    val dateRange = _dateRange.asStateFlow()

    // Pricing Assumptions States
    private val _marginPercent = MutableStateFlow(30.0)
    val marginPercent = _marginPercent.asStateFlow()

    private val _marketType = MutableStateFlow(MarketType.LOCAL)
    val marketType = _marketType.asStateFlow()

    val currentCapabilities = subscriptionStateManager.currentCapabilities
    val effectiveTier = subscriptionStateManager.effectiveTier

    private val canViewNurseryFinance: StateFlow<Boolean> = combine(
        currentCapabilities,
        subscriptionStateManager.isAdmin,
        subscriptionStateManager.isDeveloper
    ) { caps, isAdmin, isDeveloper ->
        FeatureAccessPolicy.canAccess(FeatureKey.FINANCE, caps.tier, isAdmin || isDeveloper).allowed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val effectiveOwnerType: StateFlow<String?> = combine(_ownerType, canViewNurseryFinance) { type, canViewNursery ->
        if (type == "nursery" && !canViewNursery) null else type
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _ownerType.value)

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableItems: StateFlow<List<Pair<String, String>>> = combine(
        effectiveOwnerType,
        canViewNurseryFinance
    ) { type, canViewNursery -> type to canViewNursery }
        .flatMapLatest { (type, canViewNursery) ->
            when (type) {
                "flock" -> flockRepository.allActiveFlocks.map { list -> list.map { it.syncId to it.name } }
                "incubation" -> incubationRepository.allIncubations.map { list -> list.map { it.id.toString() to "Hatch ${it.id}" } }
                "nursery" -> if (canViewNursery) nurseryRepository.activeFlocklets.map { list -> list.map { it.syncId to it.species } } else flowOf(emptyList())
                else -> flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val enrichedStats: StateFlow<EnrichedFinancialStats?> = combine(
        effectiveOwnerType, _ownerId, _dateRange
    ) { type, id, range ->
        DataParams(type, id, FinancialChartUtil.TimeBucket.DAY, range)
    }.flatMapLatest { params ->
        if (params.type != null && params.id != null) {
            repository.getEnrichedStats(params.type, params.id)
        } else {
            repository.getAggregatedStats(params.type, params.id, params.range.first, params.range.second).map {
                EnrichedFinancialStats(
                    baseStats = it,
                    trustLevel = FinancialTrustLevel.HIGH
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val chartData: StateFlow<List<FinancialChartUtil.ChartPoint>> = combine(
        effectiveOwnerType, _ownerId, _timeBucket, _dateRange
    ) { type, id, bucket, range ->
        DataParams(type, id, bucket, range)
    }.flatMapLatest { params ->
        repository.getFilteredEntries(params.type, params.id, params.range.first, params.range.second)
            .map { entries ->
                FinancialChartUtil.aggregate(entries, params.bucket, params.range.first, params.range.second)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRevenue: StateFlow<Double> = enrichedStats.map { it?.baseStats?.totalRevenue ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCost: StateFlow<Double> = enrichedStats.map { it?.baseStats?.totalCost ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalProfit: StateFlow<Double> = enrichedStats.map { it?.baseStats?.netProfit ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val avgROI: StateFlow<Double> = enrichedStats.map { it?.baseStats?.avgROI ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val costPerBird: StateFlow<Double?> = enrichedStats.map { it?.costPerBird }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profitPerBatch: StateFlow<Double?> = enrichedStats.map { it?.profitPerBatch }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trustLevel: StateFlow<FinancialTrustLevel> = enrichedStats.map { it?.trustLevel ?: FinancialTrustLevel.HIGH }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialTrustLevel.HIGH)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pricingSuggestionResult: StateFlow<PricingSuggestionResult?> = combine(
        effectiveOwnerType, _ownerId, _marginPercent, _marketType
    ) { type, id, margin, market ->
        if (type == null || id == null) null
        else PricingParams(type, id, margin, market)
    }.flatMapLatest { params ->
        if (params == null) flowOf<PricingSuggestionResult?>(null)
        else flow<PricingSuggestionResult?> {
            val itemType = when (params.type) {
                "flock" -> PricedItemType.EGG
                "incubation" -> PricedItemType.CHICK
                else -> PricedItemType.ADULT
            }
            val species = resolveSpecies(params.type, params.id)
            
            val costResult = when (params.type) {
                "flock" -> unitCostProvider.getEggUnitCost(params.id)
                "incubation" -> unitCostProvider.getChickUnitCost(params.id)
                else -> UnitCostResult.Unavailable(emptySet(), "Unsupported type")
            }
            
            emit(pricingSuggestionBuilder.build(
                costResult = costResult,
                marginPercent = params.margin,
                marketType = params.market,
                itemType = itemType,
                sourceId = params.id,
                species = species
            ))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setOwnerType(type: String?) {
        if (type == "nursery" && !canViewNurseryFinance.value) {
            _ownerType.value = null
        } else {
            _ownerType.value = type
        }
        _ownerId.value = null // Reset item when type changes
    }

    fun setOwnerId(id: String?) {
        _ownerId.value = id
    }

    fun setTimeBucket(bucket: FinancialChartUtil.TimeBucket) {
        _timeBucket.value = bucket
    }

    fun setMarginPercent(margin: Double) {
        _marginPercent.value = margin
    }

    fun setMarketType(type: MarketType) {
        _marketType.value = type
    }

    fun setDateRange(daysBack: Int) {
        val end = System.currentTimeMillis()
        val start = end - (daysBack.toLong() * 24 * 60 * 60 * 1000)
        _dateRange.value = start to end
    }

    private fun getDefaultDateRange(): Pair<Long, Long> {
        val end = System.currentTimeMillis()
        val start = end - (30L * 24 * 60 * 60 * 1000) // Default 30 days
        return start to end
    }

    private suspend fun resolveSpecies(type: String, id: String): Species {
        val speciesValue: Any? = when (type) {
            "flock" -> flockRepository.allActiveFlocks.firstOrNull()
                ?.firstOrNull { it.syncId == id }
                ?.species
            "incubation" -> incubationRepository.allIncubations.firstOrNull()
                ?.firstOrNull { it.id.toString() == id }
                ?.species
            "nursery" -> nurseryRepository.activeFlocklets.firstOrNull()
                ?.firstOrNull { it.syncId == id }
                ?.species
            else -> null
        }
        return parseSpecies(speciesValue)
    }

    private suspend fun resolveCount(type: String, id: String): Int {
        return when (type) {
            "flock" -> flockRepository.allActiveFlocks.first().find { it.syncId == id }?.eggCount ?: 0
            "incubation" -> incubationRepository.allIncubations.first().find { it.id.toString() == id }?.eggsCount ?: 0
            "nursery" -> nurseryRepository.activeFlocklets.first().find { it.syncId == id }?.chickCount ?: 0
            else -> 0
        }
    }

    private fun parseSpecies(speciesValue: Any?): Species {
        val normalized = when (speciesValue) {
            is Species -> return speciesValue
            is String -> speciesValue
            else -> null
        }
        if (normalized.isNullOrBlank()) return Species.UNKNOWN
        return runCatching { Species.valueOf(normalized.trim().uppercase(Locale.US)) }
            .getOrElse { Species.UNKNOWN }
    }

    private data class DataParams(
        val type: String?,
        val id: String?,
        val bucket: FinancialChartUtil.TimeBucket,
        val range: Pair<Long, Long>
    )

    private data class PricingParams(
        val type: String,
        val id: String,
        val margin: Double,
        val market: MarketType
    )
}
