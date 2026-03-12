package com.example.hatchtracker.feature.flock.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.domain.breeding.BreedingGoal
import com.example.hatchtracker.domain.breeding.BreedingGoalType
import com.example.hatchtracker.data.models.Bird as DataBird
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.data.models.Incubation as DataIncubation
import com.example.hatchtracker.data.models.Sex as DataSex
import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.repository.BreedingRepository
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.common.util.BreedingOptimizer
import com.example.hatchtracker.common.util.OptimizationWeights
import com.example.hatchtracker.common.util.RecommendedPair
import com.example.hatchtracker.model.Bird as DomainBird
import com.example.hatchtracker.model.Incubation as DomainIncubation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class OptimizationUiState(
    val selectedFlockIds: Set<Long> = emptySet(),
    val availableFlocks: List<Flock> = emptyList(),
    val activeSteps: OptimizationStep = OptimizationStep.SELECT_FLOCKS,
    val selectedGoals: Set<BreedingGoalType> = emptySet(),
    val weights: OptimizationWeights = OptimizationWeights(),
    val results: List<RecommendedPair> = emptyList(),
    val isCalculating: Boolean = false,
    val error: String? = null
)

enum class OptimizationStep {
    SELECT_FLOCKS,
    CONFIGURE_WEIGHTS,
    RESULTS
}

@HiltViewModel
class MultiFlockOptimizationViewModel @Inject constructor(
    private val flockRepository: FlockRepository,
    private val birdRepository: BirdRepository,
    private val breedingRepository: BreedingRepository,
    private val incubationRepository: IncubationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizationUiState())
    val uiState: StateFlow<OptimizationUiState> = _uiState.asStateFlow()

    val flocks = flockRepository.allActiveFlocks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    init {
        viewModelScope.launch {
            flocks.collect { list ->
                _uiState.update { it.copy(availableFlocks = list) }
            }
        }
    }

    fun toggleFlockSelection(flockId: Long) {
        _uiState.update { state ->
            val newSelection = if (state.selectedFlockIds.contains(flockId)) {
                state.selectedFlockIds - flockId
            } else {
                state.selectedFlockIds + flockId
            }
            state.copy(selectedFlockIds = newSelection)
        }
    }

    fun toggleGoal(goal: BreedingGoalType) {
        _uiState.update { state ->
            val newGoals = if (state.selectedGoals.contains(goal)) {
                state.selectedGoals - goal
            } else {
                state.selectedGoals + goal
            }
            state.copy(selectedGoals = newGoals)
        }
    }
    
    fun updateWeights(weights: OptimizationWeights) {
        _uiState.update { it.copy(weights = weights) }
    }

    fun nextStep() {
        _uiState.update { 
            when(it.activeSteps) {
                OptimizationStep.SELECT_FLOCKS -> it.copy(activeSteps = OptimizationStep.CONFIGURE_WEIGHTS)
                OptimizationStep.CONFIGURE_WEIGHTS -> {
                    runOptimization()
                    it.copy(activeSteps = OptimizationStep.RESULTS, isCalculating = true)
                }
                OptimizationStep.RESULTS -> it
            }
        }
    }
    
    fun reset() {
        _uiState.update { OptimizationUiState() }
    }

    private fun runOptimization() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true, error = null) }
            
            try {
                val state = _uiState.value
                val flockIds = state.selectedFlockIds.toList()
                
                val allBirds = birdRepository.activeBirds.first()
                val targetBirds = allBirds
                    .filter { flockIds.contains(it.flockId) }
                    .map { it.toDomain() }
                
                val history = breedingRepository.breedingRecords.first()
                val incubations = incubationRepository.allIncubations.first().map { it.toDomain() }
                
                // 2. Run Engine (Off-main thread)
                val results = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                     BreedingOptimizer.optimize(
                        activeBirds = targetBirds,
                        breedingHistory = history,
                        incubations = incubations,
                        goals = state.selectedGoals.map { type -> BreedingGoal(type, priority = 1) }, // Default priority 1 for selected
                        weights = state.weights
                    )
                }

                _uiState.update { 
                    it.copy(
                        results = results,
                        activeSteps = OptimizationStep.RESULTS,
                        isCalculating = false
                    ) 
                }
                
            } catch (e: Exception) {
                 _uiState.update { it.copy(error = e.message, isCalculating = false) }
            }
        }
    }

    private fun DataBird.toDomain(): DomainBird = this
    private fun DataIncubation.toDomain(): DomainIncubation = this
}

