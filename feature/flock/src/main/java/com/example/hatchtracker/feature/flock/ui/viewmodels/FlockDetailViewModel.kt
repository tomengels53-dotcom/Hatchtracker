package com.example.hatchtracker.feature.flock.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.FinancialSummary
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.repository.UserRepository
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.core.common.HatchyGuidanceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.example.hatchtracker.model.UiText

@HiltViewModel
class FlockDetailViewModel @Inject constructor(
    private val flockRepository: FlockRepository,
    private val birdRepository: BirdRepository,
    private val financialRepository: FinancialRepository,
    savedStateHandle: SavedStateHandle,
    private val subscriptionStateManager: SubscriptionStateManager,
    private val userRepository: UserRepository,
    private val lifecycleService: com.example.hatchtracker.data.service.BirdLifecycleService,
    private val eggProductionRepository: com.example.hatchtracker.data.repository.EggProductionRepository,
    private val profileImageRepository: com.example.hatchtracker.core.data.repository.ProfileImageRepository,
    val localeFormatService: com.example.hatchtracker.common.format.LocaleFormatService
) : ViewModel() {

    val currencyCode: StateFlow<String> = userRepository.userProfile
        .map { it?.currencyCode ?: "USD" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val dateFormat: StateFlow<String> = userRepository.userProfile
        .map { it?.dateFormat ?: "DD-MM-YYYY" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DD-MM-YYYY")

    private val flockId: Long = checkNotNull(savedStateHandle["flockId"])

    val flock: StateFlow<Flock?> = flockRepository.getFlockFlow(flockId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val birds: StateFlow<List<Bird>> = birdRepository.allBirds
        .map { all -> all.filter { it.flockId == flockId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val financialSummary: StateFlow<FinancialSummary?> = flock
        .flatMapLatest { flock ->
            if (flock != null) {
                financialRepository.observeFinancialSummary(flock.syncId, "flock")
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val eggProductionStats: StateFlow<List<com.example.hatchtracker.data.models.EggProductionEntity>> = flock
        .flatMapLatest { f ->
            if (f != null) {
                val sevenDaysAgo = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()) - 7
                eggProductionRepository.observeFlockProduction(f.syncId, sevenDaysAgo)
            } else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val capabilities = subscriptionStateManager.currentCapabilities

    val hatchyAdvice: StateFlow<UiText?> = kotlinx.coroutines.flow.combine(flock, birds) { f, b ->
        if (f == null) null
        else HatchyGuidanceEngine.getFlockAdvice(f.purpose, b.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Selection State
    private val _selectedBirds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedBirds: StateFlow<Set<Long>> = _selectedBirds.asStateFlow()

    // Available Flocks for Move Dialog
    val allFlocks: StateFlow<List<Flock>> = flockRepository.activeFlocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleBirdSelection(birdId: Long) {
        _selectedBirds.update { current ->
            if (current.contains(birdId)) current - birdId else current + birdId
        }
    }

    fun clearSelection() {
        _selectedBirds.value = emptySet()
    }

    fun moveSelectedBirds(targetFlockId: Long) {
        val currentFlockId = flock.value?.id ?: return
        val birdsToMove = _selectedBirds.value.toList()
        
        if (birdsToMove.isEmpty()) return

        viewModelScope.launch {
            lifecycleService.moveBirds(birdsToMove, currentFlockId, targetFlockId)
            _selectedBirds.value = emptySet()
        }
    }

    fun updateFlock(name: String, notes: String, purpose: String, active: Boolean, breeds: List<String>) {
        val current = flock.value ?: return
        viewModelScope.launch {
            flockRepository.updateFlock(current.copy(
                name = name,
                notes = notes.ifBlank { null },
                purpose = purpose,
                active = active,
                breeds = breeds
            ))
        }
    }

    fun deleteFlock(reason: String = "Manual deletion") {
        val current = flock.value ?: return
        viewModelScope.launch {
            profileImageRepository.deleteOldPhoto(current.imagePath)
            lifecycleService.removeFlock(current, reason)
        }
    }

    fun recordBirdsDeath(birdIds: List<Long>, reason: String) {
        viewModelScope.launch {
            birdIds.forEach { id ->
                lifecycleService.removeBird(id, reason)
            }
            // Clear selection after processing
            _selectedBirds.value = emptySet()
        }
    }

    suspend fun saveProfilePhoto(uri: android.net.Uri): Result<String?> {
        val current = flock.value ?: return Result.failure(Exception("Flock not loaded"))
        val result = profileImageRepository.saveProfilePhoto(uri)
        result.onSuccess { newPath ->
            flockRepository.updateFlock(current.copy(imagePath = newPath))
            profileImageRepository.deleteOldPhoto(current.imagePath)
        }
        return result
    }

    fun removeProfilePhoto() {
        viewModelScope.launch {
            val current = flock.value ?: return@launch
            if (current.imagePath != null) {
                profileImageRepository.deleteOldPhoto(current.imagePath)
                flockRepository.updateFlock(current.copy(imagePath = null))
            }
        }
    }
}
