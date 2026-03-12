package com.example.hatchtracker.feature.bird

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.data.models.FinancialSummary
import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.repository.UserRepository
import com.example.hatchtracker.data.service.BirdLifecycleService
import com.example.hatchtracker.common.format.LocaleFormatService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.hatchtracker.domain.breeding.*
import com.example.hatchtracker.model.breeding.GeneticInsightUiModel
import com.example.hatchtracker.domain.breeding.ui.GeneticInsightUiMapper
import com.example.hatchtracker.model.genetics.BreedingScenarioProfile
import com.example.hatchtracker.domain.breeding.BreedingPredictionResult
import com.example.hatchtracker.model.genetics.PhenotypeResult
import javax.inject.Inject
import com.example.hatchtracker.data.models.BirdTraitOverride
import com.example.hatchtracker.data.models.GeneticProfile
import com.example.hatchtracker.data.models.TraitCategory
import com.example.hatchtracker.data.repository.DomainEventRepository
import com.example.hatchtracker.model.DomainEvent

data class BirdTimelineEvent(
    val title: String,
    val description: String?,
    val timestamp: Long,
    val type: String
)

sealed class BirdUiEvent {
    data class ShowSnackbar(val message: String) : BirdUiEvent()
}

@HiltViewModel
class BirdDetailViewModel @Inject constructor(
    private val birdRepository: BirdRepository,
    private val flockRepository: FlockRepository,
    private val traitPromotionManager: com.example.hatchtracker.domain.logic.TraitPromotionManager,
    private val userRepository: UserRepository,
    private val lifecycleService: BirdLifecycleService,
    private val financialRepository: FinancialRepository,
    private val profileImageRepository: com.example.hatchtracker.core.data.repository.ProfileImageRepository,
    private val geneticInsightEngine: GeneticInsightEngine,
    private val breederActionInterpreter: BreederActionInterpreter,
    private val geneticInsightUiMapper: GeneticInsightUiMapper,
    private val domainEventRepository: DomainEventRepository,
    val localeFormatService: LocaleFormatService
) : ViewModel() {

    val dateFormat: StateFlow<String> = userRepository.userProfile
        .map { it?.dateFormat ?: "DD-MM-YYYY" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DD-MM-YYYY")

    val userProfile: StateFlow<com.example.hatchtracker.domain.model.UserProfile?> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _bird = MutableStateFlow<Bird?>(null)
    val bird: StateFlow<Bird?> = _bird.asStateFlow()

    private val _financialSummary = MutableStateFlow<FinancialSummary?>(null)
    val financialSummary: StateFlow<FinancialSummary?> = _financialSummary.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _geneticInsight = MutableStateFlow<GeneticInsightUiModel?>(null)
    val geneticInsight: StateFlow<GeneticInsightUiModel?> = _geneticInsight.asStateFlow()
    
    private val _timelineEvents = MutableStateFlow<List<BirdTimelineEvent>>(emptyList())
    val timelineEvents: StateFlow<List<BirdTimelineEvent>> = _timelineEvents.asStateFlow()

    private val _uiEvent = MutableSharedFlow<BirdUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun clearError() {
        _error.value = null
    }

    fun setError(errorCode: String) {
        _error.value = errorCode
    }

    fun loadBird(birdId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Observe the bird reactively
            launch {
                birdRepository.getBirdByIdFlow(birdId).collect {
                    _bird.value = it
                    _isLoading.value = false
                }
            }
            
            // Observe financials reactively
            launch {
                financialRepository.observeFinancialSummary(birdId.toString(), "bird").collect {
                    _financialSummary.value = it
                }
            }

            // Generate Genetic Insights
            runGeneticAnalysis(birdId)
            
            // Load Timeline (Reactive)
            launch {
                _bird.filterNotNull().first().let { bird ->
                    domainEventRepository.getEventsFlowForAggregate("BIRD", bird.syncId)
                        .collect { events ->
                            _timelineEvents.value = mapEventsToTimeline(events)
                        }
                }
            }
        }
    }

    private suspend fun mapEventsToTimeline(events: List<DomainEvent>): List<BirdTimelineEvent> {
        return events.map { event ->
            val payload = try {
                event.payloadJson?.let { org.json.JSONObject(it) }
            } catch (e: Exception) {
                null
            }

            val title = when (event.eventType) {
                com.example.hatchtracker.data.DomainEventLogger.BIRD_ADDED -> "Bird Added"
                com.example.hatchtracker.data.DomainEventLogger.BIRD_GRADUATED -> "Graduated from Nursery"
                com.example.hatchtracker.data.DomainEventLogger.BIRD_MOVED -> {
                    val toFlockId = payload?.optLong("targetFlockId") ?: 0L
                    val flockName = if (toFlockId > 0) flockRepository.getFlockById(toFlockId)?.name else null
                    if (flockName != null) "Moved to flock $flockName" else "Moved to another flock"
                }
                com.example.hatchtracker.data.DomainEventLogger.BIRD_REMOVED -> {
                    val category = payload?.optString("category")?.lowercase()
                    val reason = payload?.optString("reason")?.lowercase()
                    when {
                        category == "sold" || reason?.contains("sold") == true -> "Sold"
                        category == "deceased" || reason?.contains("deceased") == true || reason?.contains("death") == true -> "Deceased"
                        category == "culled" || reason?.contains("cull") == true -> "Culled"
                        else -> "Removed from records"
                    }
                }
                com.example.hatchtracker.data.DomainEventLogger.HEALTH_RECORDED -> "Health Recorded"
                com.example.hatchtracker.data.DomainEventLogger.VACCINATION_APPLIED -> "Vaccination Applied"
                com.example.hatchtracker.data.DomainEventLogger.TREATMENT_APPLIED -> "Treatment Applied"
                else -> event.eventType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            }
            
            // Extract notes from payload if present
            val description = when (event.eventType) {
                com.example.hatchtracker.data.DomainEventLogger.HEALTH_RECORDED -> payload?.optString("notes")
                com.example.hatchtracker.data.DomainEventLogger.VACCINATION_APPLIED -> payload?.optString("vaccine")
                com.example.hatchtracker.data.DomainEventLogger.TREATMENT_APPLIED -> payload?.optString("treatment")
                com.example.hatchtracker.data.DomainEventLogger.BIRD_REMOVED -> payload?.optString("reason")
                else -> payload?.optString("notes")
            }

            BirdTimelineEvent(
                title = title,
                description = if (description.isNullOrBlank()) null else description,
                timestamp = event.timestamp,
                type = event.eventType
            )
        }.sortedByDescending { it.timestamp }
    }

    private fun runGeneticAnalysis(birdId: Long) {
        viewModelScope.launch {
            try {
                _bird.filterNotNull().first().let { bird ->
                    val report = withContext(Dispatchers.Default) {
                        val syntheticPrediction = BreedingPredictionResult(
                            phenotypeResult = PhenotypeResult(probabilities = emptyList())
                        )
                        geneticInsightEngine.analyzePairing(
                            species = bird.species,
                            sire = bird,
                            dam = bird,
                            prediction = syntheticPrediction,
                            scenario = BreedingScenarioProfile(
                                goalType = "STABILIZATION",
                                breedingMode = "INDIVIDUAL_EVALUATION"
                            )
                        )
                    }
                    val contract = breederActionInterpreter.interpret(report, null)
                    val uiModel = geneticInsightUiMapper.map(contract, report)
                    _geneticInsight.value = uiModel
                }
            } catch (e: Exception) {
                // Ignore or show compact fallback
            }
        }
    }

    fun promoteTrait(bird: Bird, traitName: String, adminId: String, reason: String? = null) {
        viewModelScope.launch {
            traitPromotionManager.promoteTrait(bird, traitName, adminId, reason)
        }
    }

    fun boostConfidence(bird: Bird, newLevel: ConfidenceLevel, adminId: String, reason: String? = null) {
        viewModelScope.launch {
            traitPromotionManager.boostConfidence(bird, newLevel, adminId, reason)
        }
    }

    fun sellBird(price: Double, date: Long, buyerName: String, notes: String) {
        val current = _bird.value ?: return
        viewModelScope.launch {
            val flockId = current.flockId
            val flock = flockRepository.getFlockById(flockId ?: 0)
            
            val ownerId = flock?.syncId ?: current.syncId
            val ownerType = if (flock != null) "flock" else "adult"

            lifecycleService.markSold(
                sourceType = com.example.hatchtracker.data.models.BirdLifecycleStage.ADULT,
                sourceId = flockId ?: current.id, 
                syncId = ownerId,
                quantity = 1,
                price = price,
                date = date,
                notes = notes,
                buyerName = buyerName.ifBlank { null },
                birdIds = listOf(current.id)
            )
        }
    }


    fun updateTraitOverride(traitId: String, optionId: String?, category: TraitCategory) {
        val currentBird = _bird.value ?: return
        viewModelScope.launch {
            val currentProfile = currentBird.customGeneticProfile ?: GeneticProfile()
            val currentOverrides = currentProfile.traitOverrides.toMutableList()
            
            // Remove existing override for this trait if it exists
            currentOverrides.removeAll { it.traitId == traitId }
            
            // Add new override if optionId is not null
            if (optionId != null) {
                currentOverrides.add(
                    BirdTraitOverride(
                        traitId = traitId,
                        optionId = optionId,
                        category = category,
                        overrideTimestamp = System.currentTimeMillis()
                    )
                )
            }
            
            val updatedProfile = currentProfile.copy(traitOverrides = currentOverrides)
            val updatedBird = currentBird.copy(customGeneticProfile = updatedProfile)
            
            birdRepository.updateBird(updatedBird)
        }
    }

    fun recordDeath(reason: String) {
        val current = _bird.value ?: return
        viewModelScope.launch {
            profileImageRepository.deleteOldPhoto(current.imagePath)
            lifecycleService.removeBird(current.id, reason)
        }
    }

    suspend fun saveProfilePhoto(uri: android.net.Uri): Result<String?> {
        val current = _bird.value ?: return Result.failure(Exception("Bird not loaded"))
        val result = profileImageRepository.saveProfilePhoto(uri)
        result.onSuccess { newPath ->
            birdRepository.updateBird(current.copy(imagePath = newPath))
            profileImageRepository.deleteOldPhoto(current.imagePath)
        }
        return result
    }

    fun removeProfilePhoto() {
        val current = _bird.value ?: return
        viewModelScope.launch {
            profileImageRepository.deleteOldPhoto(current.imagePath)
            birdRepository.updateBird(current.copy(imagePath = null))
            _uiEvent.emit(BirdUiEvent.ShowSnackbar("Profile photo removed"))
        }
    }

    fun recordHealthEvent(type: String, notes: String) {
        val current = _bird.value ?: return
        viewModelScope.launch {
            lifecycleService.recordHealthEvent(current.id, type, notes)
                .onSuccess { _uiEvent.emit(BirdUiEvent.ShowSnackbar("Health event recorded")) }
                .onFailure { _uiEvent.emit(BirdUiEvent.ShowSnackbar("Failed to record health event: ${it.message}")) }
        }
    }

    fun recordVaccination(vaccineName: String) {
        val current = _bird.value ?: return
        viewModelScope.launch {
            lifecycleService.recordVaccination(current.id, vaccineName)
                .onSuccess { _uiEvent.emit(BirdUiEvent.ShowSnackbar("Vaccination recorded")) }
                .onFailure { _uiEvent.emit(BirdUiEvent.ShowSnackbar("Failed to record vaccination")) }
        }
    }

    fun recordTreatment(treatmentName: String) {
        val current = _bird.value ?: return
        viewModelScope.launch {
            lifecycleService.recordTreatment(current.id, treatmentName)
                .onSuccess { _uiEvent.emit(BirdUiEvent.ShowSnackbar("Treatment recorded")) }
                .onFailure { _uiEvent.emit(BirdUiEvent.ShowSnackbar("Failed to record treatment")) }
        }
    }

    private suspend fun refreshTimeline() {
        // No longer explicitly needed due to Flow-backed observation,
        // but keeping signature for callers if any remain (deprecated).
    }
}
