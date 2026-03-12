package com.example.hatchtracker.feature.incubation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.domain.breeding.ForecastResult
import com.example.hatchtracker.domain.breeding.HatchForecaster
import com.example.hatchtracker.domain.breeding.HistoricalPoint
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.data.repository.IncubationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class HatchPlannerUiState(
    val plannedEggs: Int = 50,
    val plannedDuration: Int = 21,
    val assumedHatchRate: Float = 85f,
    val isLoading: Boolean = true,
    val forecast: ForecastResult? = null,
    val historyCount: Int = 0
)

@HiltViewModel
class HatchPlannerViewModel @Inject constructor(
    private val incubationRepository: IncubationRepository,
    private val financialRepository: FinancialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HatchPlannerUiState())
    val uiState: StateFlow<HatchPlannerUiState> = _uiState.asStateFlow()

    private var history: List<HistoricalPoint> = emptyList()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val completedIncubations = incubationRepository.getAllIncubations().filter { it.hatchCompleted }
            val allCosts = financialRepository.getIncubationCosts()
            
            history = completedIncubations.map { incubation ->
                HistoricalPoint(
                    incubation = incubation,
                    entries = allCosts.filter { it.ownerId == incubation.syncId }
                )
            }
            
            _uiState.update { it.copy(
                isLoading = false,
                historyCount = history.size
            ) }
            
            calculateForecast()
        }
    }

    fun updatePlannedEggs(count: Int) {
        _uiState.update { it.copy(plannedEggs = count) }
        calculateForecast()
    }

    fun updateAssumedHatchRate(rate: Float) {
        _uiState.update { it.copy(assumedHatchRate = rate) }
        calculateForecast()
    }

    fun updatePlannedDuration(days: Int) {
        _uiState.update { it.copy(plannedDuration = days) }
        calculateForecast()
    }

    private fun calculateForecast() {
        val state = _uiState.value
        val result = HatchForecaster.predict(
            plannedEggs = state.plannedEggs,
            plannedDurationDays = state.plannedDuration,
            assumedHatchRate = state.assumedHatchRate,
            history = history
        )
        _uiState.update { it.copy(forecast = result) }
    }
}





