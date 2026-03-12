package com.example.hatchtracker.feature.incubation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.repository.IncubationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.example.hatchtracker.data.service.BirdLifecycleService

@HiltViewModel
class AddIncubationViewModel @Inject constructor(
    private val repository: IncubationRepository,
    private val lifecycleService: BirdLifecycleService,
    private val birdRepository: com.example.hatchtracker.data.repository.BirdRepository,
    private val flockRepository: com.example.hatchtracker.data.repository.FlockRepository,
    private val featureAccess: com.example.hatchtracker.billing.FeatureAccess,
    private val subscriptionManager: com.example.hatchtracker.billing.SubscriptionStateManager,
    private val breederRepository: com.example.hatchtracker.data.repository.BreedStandardRepository,
    private val deviceRepository: com.example.hatchtracker.data.repository.DeviceRepository,
    private val deviceCapacityManager: com.example.hatchtracker.domain.breeding.DeviceCapacityManager,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    val localeFormatService: com.example.hatchtracker.common.format.LocaleFormatService,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {
    val currentCapabilities = subscriptionManager.currentCapabilities
    
    // Load user devices (incubators only) with capacity
    val userIncubators = deviceCapacityManager.getCapacityForDevices(
        deviceRepository.getUserDevices().map { devices ->
            devices.filter { it.type == com.example.hatchtracker.model.DeviceType.SETTER || it.type == com.example.hatchtracker.model.DeviceType.HATCHER }
        }
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dateFormat: kotlinx.coroutines.flow.StateFlow<String> = userRepository.userProfile
        .map { it?.dateFormat ?: "DD-MM-YYYY" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DD-MM-YYYY")

    fun getFlockById(flockId: Long) = flockRepository.getFlockFlow(flockId)
    
    private val _selectedBreed = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val selectedBreed = _selectedBreed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSelectedBreed(result: com.example.hatchtracker.core.navigation.BreedSelectionResult) {
        _selectedBreed.value = result.breedName
    }

    fun setBreedName(name: String) {
        _selectedBreed.value = name
    }

    fun clearSelectedBreed() {
        _selectedBreed.value = null
    }
    // Helper state could be moved here if we want to manage form state in VM
    // For now we prioritize repository access

    fun saveIncubation(incubation: Incubation, onSuccess: (Incubation) -> Unit) {
        viewModelScope.launch {
            val defaultMixed = appContext.getString(com.example.hatchtracker.core.ui.R.string.mixed_unknown_breed)
            val finalBreeds = _selectedBreed.value?.let { listOf(it) } ?: listOf(defaultMixed)
            val finalIncubation = incubation.copy(breeds = finalBreeds)
            val saved = lifecycleService.registerIncubation(finalIncubation)
            onSuccess(saved)
        }
    }

    suspend fun getActiveIncubationCount(): Int {
        return repository.getActiveIncubationCount()
    }
    
    val birds = birdRepository.allBirds
    val activeFlocks = flockRepository.activeFlocks
    
    fun canUseParentLinking(): Boolean {
        return featureAccess.canUseParentLinking()
    }

    fun canCreateMoreIncubations(currentCount: Int): Boolean {
        return featureAccess.canCreateMoreThanNIncubations(currentCount)
    }
    
    /**
     * Get all species with gating information based on current subscription tier.
     */
    fun getSpeciesUiRows(): List<com.example.hatchtracker.domain.util.SpeciesUiRow> {
        val caps = currentCapabilities.value
        val isAdminOrDev = subscriptionManager.isAdmin.value || subscriptionManager.isDeveloper.value
        
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
        val isAdminOrDev = subscriptionManager.isAdmin.value || subscriptionManager.isDeveloper.value
        
        return com.example.hatchtracker.domain.util.SpeciesGatingHelper.canUseSpecies(
            speciesId = speciesId,
            tier = caps.tier,
            isAdminOrDeveloper = isAdminOrDev
        )
    }

    /**
     * Check if egg count is within tier limits.
     */
    fun canAddMoreEggs(count: Int): Boolean {
        return count <= currentCapabilities.value.maxEggsPerIncubation
    }
}






