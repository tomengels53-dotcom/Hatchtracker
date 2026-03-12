package com.example.hatchtracker.feature.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.models.SalesBatch
import com.example.hatchtracker.data.repository.SalesBatchRepository
import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.service.BirdLifecycleService
import com.example.hatchtracker.domain.breeding.MarketType
import com.example.hatchtracker.domain.pricing.*
import com.example.hatchtracker.domain.pricing.unitcost.UnitCostResult
import com.example.hatchtracker.model.Species
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddSalesBatchViewModel @Inject constructor(
    private val repository: SalesBatchRepository,
    private val lifecycleService: BirdLifecycleService,
    private val birdRepository: BirdRepository,
    private val nurseryRepository: com.example.hatchtracker.data.repository.NurseryRepository,
    private val unitCostProvider: UnitCostProvider,
    private val pricingSuggestionBuilder: PricingSuggestionBuilder,
    private val subscriptionStateManager: SubscriptionStateManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val ownerId: String = savedStateHandle["ownerId"] ?: ""
    private val ownerType: String = savedStateHandle["ownerType"] ?: "flock"

    private val birdIds: List<Long> = savedStateHandle.get<String>("birdIds")
        ?.split(",")
        ?.mapNotNull { it.toLongOrNull() } ?: emptyList()

    private val _itemType = MutableStateFlow(if (ownerType == "flocklet") "chick" else "bird") // "chick", "egg", "adult" (mapped to PricedItemType)
    val itemType = _itemType.asStateFlow()

    private val _quantity = MutableStateFlow(if (birdIds.isNotEmpty()) birdIds.size.toString() else "1")
    val quantity = _quantity.asStateFlow()

    private val _selectedBirds = MutableStateFlow<List<Bird>>(emptyList())
    val selectedBirds = _selectedBirds.asStateFlow()

    private val _unitPrice = MutableStateFlow("0.00")
    val unitPrice = _unitPrice.asStateFlow()

    private val _buyerType = MutableStateFlow("private")
    val buyerType = _buyerType.asStateFlow()

    private val _buyerName = MutableStateFlow("")
    val buyerName = _buyerName.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _desiredMargin = MutableStateFlow(30.0)
    val desiredMargin = _desiredMargin.asStateFlow()

    private val _marketType = MutableStateFlow(MarketType.LOCAL)
    val marketType = _marketType.asStateFlow()

    val isQuantityLocked = MutableStateFlow(birdIds.isNotEmpty())

    private val _pricingSuggestionResult = MutableStateFlow<PricingSuggestionResult?>(null)
    val pricingSuggestionResult = _pricingSuggestionResult.asStateFlow()

    val capabilities = subscriptionStateManager.currentCapabilities

    private var historicalAvgPrice: Double? = null

    init {
        loadSelectedBirds()
        observeInputsForPricing()
    }

    private fun loadSelectedBirds() {
        if (birdIds.isEmpty()) return
        viewModelScope.launch {
            val birds = birdIds.mapNotNull { birdRepository.getBirdById(it) }
            _selectedBirds.value = birds
        }
    }

    private fun observeInputsForPricing() {
        viewModelScope.launch {
            combine(_itemType, _desiredMargin, _marketType) { type, margin, market ->
                Triple(type, margin, market)
            }.collectLatest { (type, margin, market) ->
                updateSuggestion(type, margin, market)
            }
        }
    }

    private suspend fun updateSuggestion(typeKey: String, margin: Double, market: MarketType) {
        if (!subscriptionStateManager.currentCapabilities.value.isPricingStrategyEnabled) {
             _pricingSuggestionResult.value = null
             return
        }

        // 1. Map Item Type
        val pricedItemType = when (typeKey.lowercase()) {
            "egg", "eggs" -> PricedItemType.EGG
            "chick", "chicks" -> PricedItemType.CHICK
            "adult", "bird" -> PricedItemType.ADULT
            else -> {
                _pricingSuggestionResult.value = PricingSuggestionResult.Unavailable(emptySet(), "Unknown item type for pricing")
                return
            }
        }

        // 2. Fetch Historical Price
        historicalAvgPrice = repository.getAverageSalePrice(typeKey)

        // 3. Get Unit Cost
        val costResult = when (pricedItemType) {
            PricedItemType.EGG -> {
                if (ownerType == "flock" && ownerId.isNotBlank()) {
                     unitCostProvider.getEggUnitCost(
                         flockId = ownerId,
                         daysBack = 30
                    )
                } else UnitCostResult.Unavailable(emptySet(), "Invalid context for Egg pricing")
            }
            PricedItemType.CHICK -> {
                  if (ownerType == "incubation" && ownerId.isNotBlank()) {
                     unitCostProvider.getChickUnitCost(ownerId)
                  } else if (ownerType == "flocklet" && ownerId.isNotBlank()) {
                     val flockletId = ownerId.toLongOrNull()
                     val f = flockletId?.let { nurseryRepository.getFlockletById(it) }
                     val hatchId = f?.hatchId?.toString()
                     if (hatchId != null) {
                        unitCostProvider.getChickUnitCost(hatchId)
                     } else UnitCostResult.Unavailable(emptySet(), "No hatch source for this flocklet")
                  } else UnitCostResult.Unavailable(emptySet(), "Invalid context for Chick pricing")
            }
             else -> UnitCostResult.Unavailable(emptySet(), "Pricing not supported for this type")
        }

        // 4. Build Suggestion
        _pricingSuggestionResult.value = pricingSuggestionBuilder.build(
            costResult = costResult,
            marginPercent = margin,
            marketType = market,
            itemType = pricedItemType,
            sourceId = ownerId,
            species = _selectedBirds.value.firstOrNull()?.species ?: Species.UNKNOWN,
            historicalAvg = historicalAvgPrice
        )
    }

    fun setDesiredMargin(value: Double) {
        _desiredMargin.value = value
    }

    fun setMarketType(value: MarketType) {
        _marketType.value = value
    }

    fun useSuggestedPrice() {
        val result = _pricingSuggestionResult.value
        if (result is PricingSuggestionResult.Available) {
            _unitPrice.value = String.format("%.2f", result.suggestion.suggestedUnitPrice)
        }
    }

    val totalPrice = combine(_quantity, _unitPrice) { q, u ->
        val quantityInt = q.toIntOrNull() ?: 0
        val priceDouble = u.toDoubleOrNull() ?: 0.0
        quantityInt * priceDouble
    }

    fun setItemType(type: String) {
        _itemType.value = type
    }

    fun setQuantity(value: String) {
        _quantity.value = value
    }

    fun setUnitPrice(value: String) {
        _unitPrice.value = value
    }

    fun setBuyerType(value: String) {
        _buyerType.value = value
    }

    fun setBuyerName(value: String) {
        _buyerName.value = value
    }

    fun setNotes(value: String) {
        _notes.value = value
    }

    fun saveSale(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val qty = _quantity.value.toIntOrNull() ?: return onError("Invalid quantity")
        val unit = _unitPrice.value.toDoubleOrNull() ?: return onError("Invalid price")
        if (qty <= 0) return onError("Quantity must be greater than zero")

        viewModelScope.launch {
            _isSaving.value = true
            val batch = SalesBatch(
                ownerId = ownerId,
                ownerType = ownerType,
                itemType = _itemType.value,
                quantity = qty,
                unitPrice = unit,
                totalPrice = qty * unit,
                buyerType = _buyerType.value,
                buyerName = _buyerName.value.ifBlank { null },
                saleDate = System.currentTimeMillis(),
                notes = _notes.value
            )

            val result = lifecycleService.sellSalesBatch(batch, birdIds.takeIf { it.isNotEmpty() })
            _isSaving.value = false

            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}
