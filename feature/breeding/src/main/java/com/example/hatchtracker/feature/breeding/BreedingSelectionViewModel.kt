package com.example.hatchtracker.feature.breeding

import com.example.hatchtracker.model.Bird as DomainBird

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Sex
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.data.models.BreedingGoal
import com.example.hatchtracker.data.models.BreedingGoalType
import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.data.repository.BreedingRepository
import com.example.hatchtracker.core.billing.BreedingIntelligenceService
import com.example.hatchtracker.data.models.BasicRecommendation
import com.example.hatchtracker.data.models.StrategicRecommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.hatchtracker.model.BreedingSafeguard
import com.example.hatchtracker.domain.breeding.BreedingSafeguardManager
import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.core.ui.R
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class BreedingSelectionUiState(
    val flocks: List<Flock> = emptyList(),
    val birdsByFlock: Map<Long, List<Bird>> = emptyMap(),
    val incubations: List<Incubation> = emptyList(),
    val selectedMale: Bird? = null,
    val selectedFemales: List<Bird> = emptyList(),
    val selectedGoals: Set<BreedingGoalType> = emptySet(),
    val basicRecommendation: BasicRecommendation? = null,
    val strategicRecommendation: StrategicRecommendation? = null,
    val isPROUser: Boolean = false,
    val excessiveMaleReuse: Boolean = false,
    val lowDiversityDetected: Boolean = false,
    val isConfirmDialogOpen: Boolean = false,
    val isProcessing: Boolean = false,
    val confirmedRecordId: Long? = null,
    val isLoading: Boolean = true,
    val safeguard: BreedingSafeguard = BreedingSafeguard.None,
    val hasAcknowledgedSafeguard: Boolean = false,
    val hatchyAdvice: UiText? = null,
    val error: UiText? = null,
    val isScenarioMode: Boolean = false,
    val scenarioRecommendations: List<BasicRecommendation> = emptyList(),
    // Scenario Mode State
    val selectedSpecies: String? = null,
    val selectedScenarioBreeds: List<com.example.hatchtracker.data.models.BreedStandard> = emptyList(),
    val breedingInsights: List<BreedingInsight> = emptyList()
)

@HiltViewModel
class BreedingSelectionViewModel @Inject constructor(
    private val flockRepository: FlockRepository,
    private val birdRepository: BirdRepository,
    private val incubationRepository: IncubationRepository,
    private val breedingRepository: BreedingRepository,
    private val breedingIntelligenceService: BreedingIntelligenceService,
    private val breedStandardRepository: com.example.hatchtracker.data.repository.BreedStandardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BreedingSelectionUiState())
    val uiState: StateFlow<BreedingSelectionUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isPROUser = breedingIntelligenceService.canAccessStrategicFeatures()) }
        loadData()
    }
    
    fun enableScenarioMode() {
        _uiState.update { it.copy(isScenarioMode = true) }
    }

    fun disableScenarioMode() {
        _uiState.update { 
            it.copy(
                isScenarioMode = false,
                selectedSpecies = null,
                selectedScenarioBreeds = emptyList(),
                selectedGoals = emptySet()
            ) 
        }
    }

    fun setScenarioSpecies(species: String) {
        _uiState.update { it.copy(selectedSpecies = species, selectedScenarioBreeds = emptyList()) }
        updatePredictionsAndWarnings()
    }

    fun addScenarioBreed(breed: com.example.hatchtracker.data.models.BreedStandard) {
        _uiState.update { 
            if (!it.selectedScenarioBreeds.any { b -> b.id == breed.id }) {
                it.copy(selectedScenarioBreeds = it.selectedScenarioBreeds + breed)
            } else it
        }
        updatePredictionsAndWarnings()
    }

    fun removeScenarioBreed(breed: com.example.hatchtracker.data.models.BreedStandard) {
        _uiState.update { 
            it.copy(selectedScenarioBreeds = it.selectedScenarioBreeds.filter { b -> b.id != breed.id })
        }
        updatePredictionsAndWarnings()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                flockRepository.allActiveFlocks,
                birdRepository.activeBirds,
                incubationRepository.getAllIncubationsFlow()
            ) { flocks, birds, incubations ->
                val grouped = birds.filter { it.flockId != null }
                    .groupBy { it.flockId!! }
                
                _uiState.update { 
                    it.copy(
                        flocks = flocks,
                        birdsByFlock = grouped,
                        incubations = incubations,
                        isLoading = false
                    )
                }
                updatePredictionsAndWarnings()
            }.collect()
        }
    }

    fun toggleGoal(goalType: BreedingGoalType) {
        _uiState.update { state ->
            val newGoals = if (state.selectedGoals.contains(goalType)) {
                state.selectedGoals - goalType
            } else {
                state.selectedGoals + goalType
            }
            state.copy(selectedGoals = newGoals)
        }
        updatePredictionsAndWarnings()
    }

    fun handleBreedSelectionResult(breedIdOrName: String) {
        viewModelScope.launch {
            // Try to find by ID first, then fallback to name within selected species.
            val breed = breedStandardRepository.getBreedById(breedIdOrName)
                ?: _uiState.value.selectedSpecies
                    ?.let { breedStandardRepository.getBreedsForSpecies(it) }
                    ?.find { it.name.equals(breedIdOrName, ignoreCase = true) }
             
            if (breed != null) {
                addScenarioBreed(breed)
            }
        }
    }

    fun toggleSelection(bird: Bird) {
        val currentState = _uiState.value
        if (bird.status != "active") return // Cannot select breeding or sold birds
        
        when (bird.sex) {
            Sex.MALE -> {
                val newMale = if (currentState.selectedMale?.id == bird.id) null else bird
                val updatedFemales = if (newMale != null) {
                    currentState.selectedFemales.filter { it.species == newMale.species }
                } else {
                    currentState.selectedFemales
                }

                _uiState.update { 
                    it.copy(
                        selectedMale = newMale,
                        selectedFemales = updatedFemales
                    )
                }
            }
            Sex.FEMALE -> {
                val isSelected = currentState.selectedFemales.any { it.id == bird.id }
                val newFemales = if (isSelected) {
                    currentState.selectedFemales.filter { it.id != bird.id }
                } else {
                    if (currentState.selectedMale == null || currentState.selectedMale.species == bird.species) {
                        currentState.selectedFemales + bird
                    } else {
                        return
                    }
                }
                
                _uiState.update { it.copy(selectedFemales = newFemales) }
            }
            else -> {}
        }
        updatePredictionsAndWarnings()
    }

    private fun updatePredictionsAndWarnings() {
        val state = _uiState.value
        val male = state.selectedMale
        val females = state.selectedFemales
        
        val excessiveReuse = if (male != null) {
            state.incubations.count { it.fatherBirdId == male.id } >= 5
        } else false

        val maleTraits = male?.geneticProfile?.fixedTraits?.toSet() ?: emptySet()
        val lowDiversity = if (maleTraits.isNotEmpty() && females.isNotEmpty()) {
            females.map { female ->
                val femaleTraits = female.geneticProfile.fixedTraits.toSet()
                maleTraits.intersect(femaleTraits).size.toFloat() / maleTraits.size.toFloat()
            }.any { it > 0.75f }
        } else false

        // Get recommendations based on subscription tier
        val basicRec = if (male != null && females.isNotEmpty()) {
            breedingIntelligenceService.getBasicPairRecommendation(male, females.first(), state.incubations)
        } else null
        
        val strategicRec = if (male != null && females.isNotEmpty() && state.isPROUser) {
            val goals = state.selectedGoals.map { BreedingGoal(it, priority = 3) }
            breedingIntelligenceService.getStrategicRecommendation(male, females.first(), state.incubations, goals)
        } else null

        val allAvailableBirds = state.birdsByFlock.values.flatten()
        val birdMap = allAvailableBirds.associateBy { it.id }.mapValues { it.value.toDomainBird() }
        val safeguard = BreedingSafeguardManager.evaluatePair(
            male?.toDomainBird(), 
            females.map { it.toDomainBird() },
            birdMap
        )

        val hatchyAdvice = if (male != null) {
            val goalsText = state.selectedGoals
                .map { it.name.lowercase().replace('_', ' ') }
                .ifEmpty { listOf("general improvement") }
                .joinToString()
            if (state.isPROUser) {
                UiText.StringResource(
                    R.string.breeding_hatchy_scenario_advice,
                    male.species,
                    goalsText
                )
            } else {
                UiText.StringResource(R.string.breeding_hatchy_upgrade_prompt)
            }
        } else if (state.isScenarioMode && state.selectedGoals.isNotEmpty()) {
            UiText.StringResource(com.example.hatchtracker.core.common.R.string.breeding_scenario_refine_prompt)
        } else null
        
        // Scenario Mode Recommendations
        val scenarioRecs = if (state.isScenarioMode && state.selectedGoals.isNotEmpty() && male != null && females.isNotEmpty()) {
             // Simple mock implementation for Scenario Mode
             // In real app, query breedStandardRepository based on traits mapping to these goals
             // For now, return a dummy recommendation if goals exist
              state.selectedGoals.map { goal ->
                  BasicRecommendation(
                      male = male,
                      female = females.first(),
                      score = 85,
                      basicSummary = UiText.StringResource(
                      com.example.hatchtracker.core.common.R.string.breeding_scenario_match_summary,
                          goal.name
                      )
                  )
              }
        } else emptyList()

        val insights = mutableListOf<BreedingInsight>()

        if (male != null && females.any { it.species != male.species }) {
            insights.add(
                BreedingInsight(
                    severity = InsightSeverity.CRITICAL,
                    title = UiText.StringResource(R.string.breeding_insight_mismatch_title),
                    body = UiText.StringResource(R.string.breeding_insight_mismatch_body)
                )
            )
        }

        if (safeguard is BreedingSafeguard.BlockingLethal) {
            insights.add(
                BreedingInsight(
                    severity = InsightSeverity.CRITICAL,
                    title = UiText.StringResource(R.string.breeding_insight_lethal_title),
                    body = UiText.StringResource(R.string.breeding_insight_lethal_body),
                    actionHint = UiText.StringResource(R.string.breeding_insight_lethal_hint)
                )
            )
        } else if (safeguard is BreedingSafeguard.WarningInbreeding) {
            insights.add(
                BreedingInsight(
                    severity = InsightSeverity.WARNING,
                    title = UiText.StringResource(R.string.breeding_insight_inbreeding_title),
                    body = UiText.StringResource(R.string.breeding_insight_inbreeding_body),
                    actionHint = UiText.StringResource(R.string.breeding_insight_inbreeding_hint)
                )
            )
        }

        if (lowDiversity) {
            insights.add(
                BreedingInsight(
                    severity = InsightSeverity.WARNING,
                    title = UiText.StringResource(R.string.breeding_insight_diversity_title),
                    body = UiText.StringResource(R.string.breeding_insight_diversity_body),
                    actionHint = UiText.StringResource(R.string.breeding_insight_diversity_hint)
                )
            )
        }

        if (excessiveReuse) {
            insights.add(
                BreedingInsight(
                    severity = InsightSeverity.INFO,
                    title = UiText.StringResource(R.string.breeding_insight_reuse_title),
                    body = UiText.StringResource(R.string.breeding_insight_reuse_body)
                )
            )
        }

        _uiState.update { 
            it.copy(
                excessiveMaleReuse = excessiveReuse,
                lowDiversityDetected = lowDiversity,
                basicRecommendation = basicRec,
                strategicRecommendation = strategicRec,
                safeguard = safeguard,
                hatchyAdvice = hatchyAdvice,
                scenarioRecommendations = scenarioRecs,
                breedingInsights = insights.take(3)
            )
        }
    }

    fun showConfirmDialog() {
        val state = _uiState.value
        if (state.selectedMale != null && state.selectedFemales.isNotEmpty()) {
            _uiState.update { it.copy(isConfirmDialogOpen = true) }
        }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(isConfirmDialogOpen = false) }
    }

    fun confirmBreeding() {
        val currentState = _uiState.value
        val male = currentState.selectedMale ?: return
        val females = currentState.selectedFemales
        if (females.isEmpty()) return

        _uiState.update { it.copy(isProcessing = true, isConfirmDialogOpen = false) }

        viewModelScope.launch {
            try {
                val recordId = breedingRepository.createBreedingRecord(
                    sire = male,
                    dams = females,
                    flockId = male.flockId ?: 0,
                    species = male.species.name,
                    goals = currentState.selectedGoals.map { it.name }
                )
                _uiState.update { it.copy(isProcessing = false, confirmedRecordId = recordId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message?.let { msg -> UiText.DynamicString(msg) } ?: UiText.StringResource(R.string.error_unknown)) }
            }
        }
    }

    val canStartBreeding: StateFlow<Boolean> = _uiState.map { state ->
        state.selectedMale != null && 
        state.selectedFemales.isNotEmpty() &&
        state.selectedFemales.all { it.species == state.selectedMale?.species }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val speciesMismatch: StateFlow<Boolean> = _uiState.map { state ->
        val male = state.selectedMale ?: return@map false
        state.selectedFemales.any { it.species != male.species }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    private fun Bird.toDomainBird(): DomainBird = this

    fun acknowledgeSafeguard() {
        _uiState.update { it.copy(hasAcknowledgedSafeguard = true) }
        showConfirmDialog()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}










