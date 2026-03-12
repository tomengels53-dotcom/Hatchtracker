package com.example.hatchtracker.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.core.domain.models.AssetCategory
import com.example.hatchtracker.core.domain.models.AssetStatus
import com.example.hatchtracker.core.domain.models.DepreciationMethod
import com.example.hatchtracker.data.models.Device
import com.example.hatchtracker.data.models.DeviceType
import com.example.hatchtracker.core.domain.models.Asset
import com.example.hatchtracker.billing.EvaluateEquipmentAccessUseCase
import com.example.hatchtracker.data.repository.AssetRepository
import com.example.hatchtracker.data.repository.DeviceCatalogRepository
import com.example.hatchtracker.data.repository.DeviceRepository
import com.example.hatchtracker.model.CatalogDevice
import com.example.hatchtracker.model.DeviceCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val catalogRepository: DeviceCatalogRepository,
    private val assetRepository: AssetRepository,
    private val evaluateEquipmentAccessUseCase: EvaluateEquipmentAccessUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState.asStateFlow()

    private val _catalogModels = MutableStateFlow<Map<DeviceType, List<CatalogDevice>>>(emptyMap())

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            catalogRepository.getAllModels().collect { allModels ->
                _catalogModels.value = allModels.groupBy { it.deviceType }
                updateAvailableModels()
            }
        }
    }

    fun onCategorySelected(category: DeviceCategory) {
        val firstTypeInCategory = DeviceType.entries.find { it.category == category } ?: DeviceType.SETTER
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            selectedType = firstTypeInCategory,
            selectedModel = null
        )
        updateAvailableModels()
    }

    fun onDeviceTypeSelected(type: DeviceType) {
        _uiState.value = _uiState.value.copy(
            selectedType = type,
            selectedModel = null // Reset model selection
        )
        updateAvailableModels()
    }

    fun onModelSelected(model: CatalogDevice) {
        _uiState.value = _uiState.value.copy(
            selectedModel = model,
            // Auto-fill display name with model name initially
            customDisplayName = model.displayName
        )
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(customDisplayName = name)
    }

    fun onTrackAsAssetChanged(track: Boolean) {
        _uiState.value = _uiState.value.copy(trackAsAsset = track)
    }

    fun onPurchasePriceChanged(price: String) {
        _uiState.value = _uiState.value.copy(purchasePriceInput = price)
    }

    fun onResidualValueChanged(value: String) {
        _uiState.value = _uiState.value.copy(residualValueInput = value)
    }

    fun onDepreciationMethodChanged(method: DepreciationMethod) {
        _uiState.value = _uiState.value.copy(depreciationMethod = method)
    }

    fun onUsefulLifeChanged(months: String) {
        _uiState.value = _uiState.value.copy(usefulLifeMonthsInput = months)
    }

    fun onExpectedCyclesChanged(cycles: String) {
        _uiState.value = _uiState.value.copy(expectedCyclesInput = cycles)
    }

    fun onPurchaseDateChanged(date: Long?) {
        _uiState.value = _uiState.value.copy(purchaseDate = date)
    }

    private fun updateAvailableModels() {
        val state = _uiState.value
        val models = _catalogModels.value[state.selectedType] ?: emptyList()
        
        // Setup smart defaults based on device type
        val defaultMethod = if (state.selectedCategory == DeviceCategory.INCUBATION || state.selectedCategory == DeviceCategory.BROODING) {
            DepreciationMethod.CYCLE_BASED
        } else {
            DepreciationMethod.TIME_BASED
        }
        
        val defaultLife = if (state.selectedCategory == DeviceCategory.HOUSING) "60" else "36"

        _uiState.value = _uiState.value.copy(
            availableModels = models,
            depreciationMethod = defaultMethod,
            usefulLifeMonthsInput = defaultLife
        )
    }

    fun saveDevice(onSuccess: () -> Unit) {
        val state = _uiState.value
        val model = state.selectedModel ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Gating Check
            val accessResult = evaluateEquipmentAccessUseCase.invoke(
                targetType = state.selectedType
            )

            if (!accessResult.allowed) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = accessResult.formattedMessage
                )
                return@launch
            }
            
            val deviceToAdd = Device(
                type = model.deviceType,
                modelId = model.id,
                displayName = state.customDisplayName.ifBlank { model.displayName },
                capacityEggs = model.capacityEggs,
                features = model.features,
                purchaseDate = state.purchaseDate,
                purchasePrice = state.purchasePriceInput.toDoubleOrNull(),
                residualValue = state.residualValueInput.toDoubleOrNull(),
                lifecycleStatus = com.example.hatchtracker.model.DeviceLifecycleStatus.ACTIVE
            )

            val result = deviceRepository.addDevice(deviceToAdd)
            
            if (result.isSuccess && state.trackAsAsset) {
                try {
                    val deviceId = result.getOrNull() ?: throw Exception("Missing device ID")
                    val price = state.purchasePriceInput.toDoubleOrNull() ?: 0.0
                    val residual = state.residualValueInput.toDoubleOrNull() ?: 0.0
                    val expectedCycles = state.expectedCyclesInput.toIntOrNull()
                    val usefulLife = state.usefulLifeMonthsInput.toIntOrNull()

                    val category = when(state.selectedType) {
                        DeviceType.SETTER, DeviceType.INCUBATOR -> AssetCategory.INCUBATOR
                        DeviceType.HATCHER -> AssetCategory.HATCHER
                        DeviceType.BROOD_PLATE, DeviceType.HEAT_LAMP, DeviceType.HEAT_PANEL, DeviceType.BROODER -> AssetCategory.BROODER
                        DeviceType.COOP, DeviceType.RUN, DeviceType.COOP_AUTO, DeviceType.NEST_BOX -> AssetCategory.COOP
                        DeviceType.FEEDER, DeviceType.WATERER, DeviceType.WASHER, DeviceType.SANITIZER -> AssetCategory.CARE
                        DeviceType.CANDLER, DeviceType.SCALE, DeviceType.THERMOMETER, DeviceType.HYGROMETER, DeviceType.CAMERA -> AssetCategory.MONITORING
                        DeviceType.TURNER, DeviceType.HUMIDITY_CONTROLLER, DeviceType.THERMOSTAT -> AssetCategory.OTHER
                    }

                    val asset = Asset(
                        assetId = UUID.randomUUID().toString(),
                        name = deviceToAdd.displayName,
                        category = category,
                        linkedDeviceId = deviceId,
                        purchaseDateEpochMs = state.purchaseDate ?: System.currentTimeMillis(),
                        purchasePrice = price,
                        residualValue = residual,
                        depreciationMethod = state.depreciationMethod,
                        usefulLifeMonths = usefulLife,
                        expectedCycles = expectedCycles,
                        cyclesAllocatedCount = 0,
                        lastAllocatedAtEpochMs = null,
                        retiredDateEpochMs = null,
                        retirementValue = null,
                        status = AssetStatus.ACTIVE
                    )
                    assetRepository.addAsset(asset)
                } catch (e: Exception) {
                    // We successfully created the device, but failed the asset.
                    // For now, log and continue, or we could delete the device.
                }
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
            
            if (result.isSuccess) {
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }
    
    fun clearError() {
         _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

@androidx.compose.runtime.Immutable
data class AddDeviceUiState(
    val selectedCategory: DeviceCategory = DeviceCategory.INCUBATION,
    val selectedType: DeviceType = DeviceType.SETTER,
    val availableModels: List<CatalogDevice> = emptyList(),
    val selectedModel: CatalogDevice? = null,
    val customDisplayName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    
    // Capital Asset Tracking
    val trackAsAsset: Boolean = false,
    val purchasePriceInput: String = "",
    val residualValueInput: String = "0",
    val purchaseDate: Long? = System.currentTimeMillis(),
    val depreciationMethod: DepreciationMethod = DepreciationMethod.TIME_BASED,
    val usefulLifeMonthsInput: String = "36",
    val expectedCyclesInput: String = "200"
)



