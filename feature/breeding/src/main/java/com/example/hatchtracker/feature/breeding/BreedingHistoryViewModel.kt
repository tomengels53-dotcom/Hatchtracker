package com.example.hatchtracker.feature.breeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.BreedingRecord
import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.repository.BreedingRepository
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.domain.breeding.BreedingAnalyticsManager
import com.example.hatchtracker.domain.breeding.TraitProgressionPoint
import com.example.hatchtracker.domain.breeding.ConfidencePoint
import com.example.hatchtracker.model.Bird as DomainBird
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class BreedingHistoryUiState(
    val records: List<BreedingRecord> = emptyList(),
    val birds: List<Bird> = emptyList(),
    val incubations: List<Incubation> = emptyList(),
    val traitProgression: Map<String, List<TraitProgressionPoint>> = emptyMap(),
    val confidenceTrend: List<ConfidencePoint> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class BreedingHistoryViewModel @Inject constructor(
    private val breedingRepository: BreedingRepository,
    private val birdRepository: BirdRepository,
    private val incubationRepository: IncubationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BreedingHistoryUiState())
    val uiState: StateFlow<BreedingHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            combine(
                breedingRepository.getAllBreedingRecordsFlow(),
                birdRepository.activeBirds, // We might need all birds for history
                incubationRepository.getAllIncubationsFlow()
            ) { records, birds, incubations ->
                val domainBirds = birds.map { it.toDomainBird() }

                // Analytics
                val topTraits = BreedingAnalyticsManager.getTopTraits(domainBirds, domainBirds.firstOrNull()?.species?.name ?: "")
                val progression = topTraits.associateWith { trait ->
                    BreedingAnalyticsManager.getTraitProgression(domainBirds, trait)
                }
                val trend = BreedingAnalyticsManager.getConfidenceGrowthTrend(domainBirds)

                _uiState.update { 
                    it.copy(
                        records = records,
                        birds = birds,
                        incubations = incubations,
                        traitProgression = progression,
                        confidenceTrend = trend,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun deleteRecord(record: BreedingRecord) {
        viewModelScope.launch {
            breedingRepository.deleteBreedingRecord(record)
        }
    }

    private fun Bird.toDomainBird(): DomainBird = this
}






