package com.example.hatchtracker.feature.breeding.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.data.models.BreedingProgram
import com.example.hatchtracker.data.models.BreedingProgramStatus
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.repository.BreedingProgramRepository
import com.example.hatchtracker.domain.breeding.BreedingGoalTemplate
import com.example.hatchtracker.domain.breeding.BreedingProgramConverter
import com.example.hatchtracker.domain.breeding.BreedingStrategyService
import com.example.hatchtracker.domain.breeding.GenerationEstimator
import com.example.hatchtracker.domain.breeding.GoalTemplateCatalog
import com.example.hatchtracker.domain.breeding.StarterPlanService
import com.example.hatchtracker.domain.breeding.StrategyRequest
import com.example.hatchtracker.domain.breeding.plan.BreedingPlanDraft
import com.example.hatchtracker.domain.breeding.plan.PlanConstraints
import com.example.hatchtracker.domain.breeding.plan.ProgramMode
import com.example.hatchtracker.data.models.GenEstimate
import com.example.hatchtracker.data.models.StartingSituation
import com.example.hatchtracker.data.models.StrategyConfig
import com.example.hatchtracker.data.models.StrategyMode
import com.example.hatchtracker.data.models.TraitDomain
import com.example.hatchtracker.data.models.GoalSpec
import com.example.hatchtracker.model.Species
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.hatchtracker.domain.breeding.*
import com.example.hatchtracker.model.breeding.GeneticInsightUiModel
import com.example.hatchtracker.domain.breeding.ui.GeneticInsightUiMapper
import com.example.hatchtracker.domain.breeding.BreedingPredictionResult
import com.example.hatchtracker.model.genetics.BreedingScenarioProfile
import com.example.hatchtracker.model.genetics.PhenotypeResult
import com.example.hatchtracker.model.Sex
import javax.inject.Inject

enum class GoalWizardStep {
    SPECIES,
    SITUATION,
    FLOCKS,
    TRAIT_DOMAINS,
    TRAIT_SPECS,
    STRATEGY_MODE,
    ANALYSIS,
    DRAFT
}

sealed interface RefinementUiState {
    data object Loading : RefinementUiState
    data class Ready(val specs: List<GoalSpec>) : RefinementUiState
    data object Empty : RefinementUiState
    data class Error(val message: String) : RefinementUiState
}

@androidx.compose.runtime.Immutable
data class BreedingProgramWizardUiState(
    val currentStep: GoalWizardStep = GoalWizardStep.SPECIES,
    val selectedSpecies: Species? = null,
    val strategyConfig: StrategyConfig = StrategyConfig(),
    val genEstimate: GenEstimate? = null,
    val availableTemplates: List<BreedingGoalTemplate> = emptyList(),
    val selectedTemplate: BreedingGoalTemplate? = null,
    val planType: ProgramMode = ProgramMode.FLOCK_BASED,
    val availableFlocks: List<Flock> = emptyList(),
    val selectedFlockIds: Set<String> = emptySet(),
    val draft: BreedingPlanDraft? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPro: Boolean = false,
    val refinementState: RefinementUiState = RefinementUiState.Loading,
    val geneticInsight: GeneticInsightUiModel? = null
)

@HiltViewModel
@OptIn(FlowPreview::class)
class BreedingProgramWizardViewModel @Inject constructor(
    private val breedingStrategyService: BreedingStrategyService,
    private val starterPlanService: StarterPlanService,
    private val goalTemplateCatalog: GoalTemplateCatalog,
    private val flockRepository: FlockRepository,
    private val actionPlanRepository: BreedingProgramRepository,
    private val sessionManager: com.example.hatchtracker.auth.SessionManager,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    private val generationEstimator: GenerationEstimator,
    private val geneticInsightEngine: GeneticInsightEngine,
    private val breederActionInterpreter: BreederActionInterpreter,
    private val geneticInsightUiMapper: GeneticInsightUiMapper,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<BreedingProgramWizardUiState> = _uiState.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<String>()
    val saveSuccess: SharedFlow<String> = _saveSuccess.asSharedFlow()

    init {
        viewModelScope.launch {
            userRepository.userProfile.collect { profile ->
                _uiState.update { it.copy(isPro = profile?.subscriptionActive == true || profile?.isDeveloper == true || profile?.isSystemAdmin == true) }
            }
        }
        // Load flocks if species was restored
        _uiState.value.selectedSpecies?.let { loadFlocks(it) }

        // Debounced Genetic Insight Trigger
        viewModelScope.launch {
            _uiState
                .map { it.strategyConfig to it.selectedFlockIds }
                .distinctUntilChanged()
                .debounce { (config, _) -> 
                    // Immediate for some types of changes, 300ms for others
                    if (config.goalSpecs.isEmpty()) 0L else 300L 
                }
                .collect { 
                    runGeneticAnalysis() 
                }
        }
    }

    private val insightCache = object : android.util.LruCache<String, GeneticInsightUiModel>(50) {}

    private fun restoreState(): BreedingProgramWizardUiState {
        val step = savedStateHandle.get<GoalWizardStep>("currentStep") ?: GoalWizardStep.SPECIES
        val species = savedStateHandle.get<Species>("selectedSpecies")
        val situation = savedStateHandle.get<StartingSituation>("startingSituation") ?: StartingSituation.COMBINE_FLOCKS
        val domains = savedStateHandle.get<List<TraitDomain>>("selectedTraitDomains")?.toSet() ?: emptySet()
        val specs = savedStateHandle.get<List<GoalSpec>>("selectedGoalSpecs") ?: emptyList()
        val stratMode = savedStateHandle.get<StrategyMode>("strategyMode") ?: StrategyMode.STRICT_LINE_BREEDING
        val template = savedStateHandle.get<BreedingGoalTemplate>("selectedTemplate")
        val mode = savedStateHandle.get<ProgramMode>("planType") ?: ProgramMode.FLOCK_BASED
        val flockIds = savedStateHandle.get<List<String>>("selectedFlockIds")?.toSet() ?: emptySet()
        
        return BreedingProgramWizardUiState(
            currentStep = step,
            selectedSpecies = species,
            strategyConfig = StrategyConfig(
                startingSituation = situation,
                goalSpecs = specs,
                strategyMode = stratMode
            ),
            selectedTemplate = template,
            planType = mode,
            selectedFlockIds = flockIds,
            availableTemplates = species?.let { goalTemplateCatalog.getTemplatesForSpecies(it) } ?: emptyList()
        )
    }

    private fun persistState(state: BreedingProgramWizardUiState) {
        savedStateHandle["currentStep"] = state.currentStep
        savedStateHandle["selectedSpecies"] = state.selectedSpecies
        savedStateHandle["startingSituation"] = state.strategyConfig.startingSituation
        savedStateHandle["selectedTraitDomains"] = state.strategyConfig.goalSpecs.map { it.domain }.distinct()
        savedStateHandle["selectedGoalSpecs"] = state.strategyConfig.goalSpecs
        savedStateHandle["strategyMode"] = state.strategyConfig.strategyMode
        savedStateHandle["selectedTemplate"] = state.selectedTemplate
        savedStateHandle["planType"] = state.planType
        savedStateHandle["selectedFlockIds"] = state.selectedFlockIds.toList()
    }

    fun confirmDraft() {
        val state = _uiState.value
        val draft = state.draft ?: return
        val user = sessionManager.getCurrentUser() ?: return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val actionPlan = BreedingProgram(
                    ownerUserId = user.uid,
                    name = state.selectedTemplate?.title ?: "Breeding Plan",
                    planSpecies = draft.species,
                    steps = draft.steps,
                    finalGeneration = draft.steps.maxByOrNull { it.generation }?.generation ?: 1,
                    status = BreedingProgramStatus.ACTIVE
                )

                val result = actionPlanRepository.createPlan(actionPlan)
                result.onSuccess { planId ->
                    _saveSuccess.emit(planId)
                    _uiState.update { it.copy(isLoading = false) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setSpecies(species: Species) {
        _uiState.update { 
            it.copy(
                selectedSpecies = species,
                availableTemplates = goalTemplateCatalog.getTemplatesForSpecies(species),
                currentStep = GoalWizardStep.SITUATION
            ).also { newState -> persistState(newState) }
        }
        loadFlocks(species)
    }

    fun setStartingSituation(situation: StartingSituation) {
        _uiState.update { 
            it.copy(
                strategyConfig = it.strategyConfig.copy(startingSituation = situation),
                currentStep = if (situation == StartingSituation.START_FROM_SCRATCH) GoalWizardStep.TRAIT_DOMAINS else GoalWizardStep.FLOCKS
            ).also { newState -> persistState(newState) }
        }
    }

    fun proceedToTraits() {
        _uiState.update { 
            it.copy(currentStep = GoalWizardStep.TRAIT_DOMAINS)
                .also { newState -> persistState(newState) }
        }
    }

    fun toggleTraitDomain(domain: TraitDomain) {
        _uiState.update { state ->
            val set = state.strategyConfig.goalSpecs.map { it.domain }.toMutableSet()
            if (set.contains(domain)) {
                set.remove(domain)
            } else {
                set.add(domain)
            }
            // Reconstruct specs to match domains (MVP logic)
            val newSpecs = set.map { d -> 
                state.strategyConfig.goalSpecs.find { it.domain == d } ?: GoalSpec(d, "general", "high", 3)
            }
            state.copy(strategyConfig = state.strategyConfig.copy(goalSpecs = newSpecs)).also { newState -> persistState(newState) }
        }
    }

    fun proceedToTraitSpecs() {
        _uiState.update { 
            it.copy(
                currentStep = GoalWizardStep.TRAIT_SPECS,
                refinementState = RefinementUiState.Loading
            ).also { newState -> persistState(newState) }
        }
        
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(300) // Simulated processing
                val currentSpecs = _uiState.value.strategyConfig.goalSpecs
                if (currentSpecs.isEmpty()) {
                    _uiState.update { 
                        it.copy(refinementState = RefinementUiState.Empty) 
                        .also { newState -> persistState(newState) }
                    }
                } else {
                    _uiState.update { 
                        it.copy(refinementState = RefinementUiState.Ready(currentSpecs)) 
                        .also { newState -> persistState(newState) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(refinementState = RefinementUiState.Error(e.message ?: "Failed to process traits")) 
                    .also { newState -> persistState(newState) }
                }
            }
        }
    }

    fun retryRefinement() {
        proceedToTraitSpecs()
    }

    fun setGoalSpecs(specs: List<GoalSpec>) {
        _uiState.update { 
            it.copy(
                strategyConfig = it.strategyConfig.copy(goalSpecs = specs),
                currentStep = GoalWizardStep.STRATEGY_MODE
            ).also { newState -> persistState(newState) }
        }
    }

    fun setStrategyMode(mode: StrategyMode) {
        _uiState.update { 
            it.copy(
                strategyConfig = it.strategyConfig.copy(strategyMode = mode),
                currentStep = GoalWizardStep.ANALYSIS
            ).also { newState -> persistState(newState) }
        }
        runAnalysis()
    }

    private fun runAnalysis() {
        val state = _uiState.value
        val estimate = generationEstimator.estimate(
            config = state.strategyConfig
        )
        _uiState.update { it.copy(genEstimate = estimate) }
        runGeneticAnalysis()
    }

    private fun runGeneticAnalysis() {
        val state = _uiState.value
        val species = state.selectedSpecies ?: return
        if (state.selectedFlockIds.isEmpty() && state.strategyConfig.startingSituation != StartingSituation.START_FROM_SCRATCH) return

        val cacheKey = "${state.selectedFlockIds.sorted().joinToString()}_${state.strategyConfig.hashCode()}"
        val cached = insightCache.get(cacheKey)
        if (cached != null) {
            _uiState.update { it.copy(geneticInsight = cached) }
            return
        }

        _uiState.update { it.copy(geneticInsight = it.geneticInsight?.copy(isLoading = true)) }

        viewModelScope.launch {
            try {
                val report = withContext(Dispatchers.Default) {
                    val syntheticSire = com.example.hatchtracker.model.Bird(species = species, sex = Sex.MALE)
                    val syntheticDam = com.example.hatchtracker.model.Bird(species = species, sex = Sex.FEMALE)
                    val syntheticPrediction = BreedingPredictionResult(
                        phenotypeResult = PhenotypeResult(probabilities = emptyList())
                    )
                    // In a real app, we'd use actual Bird objects from the selected flocks
                    // For now, we simulate the population report based on species and config
                    geneticInsightEngine.analyzePairing(
                        species = species,
                        sire = syntheticSire,
                        dam = syntheticDam,
                        prediction = syntheticPrediction,
                        scenario = BreedingScenarioProfile(
                            goalType = state.strategyConfig.strategyMode.name,
                            breedingMode = "POPULATION",
                            variabilityTolerance = 0.5
                        )
                    )
                }

                val contract = breederActionInterpreter.interpret(report, null)
                val uiModel = geneticInsightUiMapper.map(contract, report)

                insightCache.put(cacheKey, uiModel)
                _uiState.update { 
                    if (state.selectedFlockIds.sorted().joinToString() + "_" + state.strategyConfig.hashCode() == cacheKey) {
                        it.copy(geneticInsight = uiModel)
                    } else it
                }
            } catch (e: Exception) {
                // Silently fail or show "Limited Intelligence"
                _uiState.update { it.copy(geneticInsight = it.geneticInsight?.copy(isLoading = false)) }
            }
        }
    }

    private fun loadFlocks(species: Species) {
        viewModelScope.launch {
            flockRepository.getFlocksBySpecies(species.name).collect { flocks ->
                _uiState.update { it.copy(availableFlocks = flocks) }
            }
        }
    }

    fun selectTemplate(template: BreedingGoalTemplate) {
        _uiState.update { 
            it.copy(selectedTemplate = template, currentStep = GoalWizardStep.STRATEGY_MODE)
                .also { newState -> persistState(newState) }
        }
    }

    fun setPlanType(type: ProgramMode) {
        _uiState.update { 
            it.copy(
                planType = type,
                currentStep = if (type == ProgramMode.FLOCK_BASED) GoalWizardStep.FLOCKS else GoalWizardStep.DRAFT
            ).also { newState -> persistState(newState) }
        }
        if (type == ProgramMode.STARTER_GOAL_ONLY) {
            generatePlan()
        }
    }

    fun toggleFlock(flockId: String) {
        _uiState.update { state ->
            val set = state.selectedFlockIds.toMutableSet()
            if (set.contains(flockId)) set.remove(flockId) else set.add(flockId)
            state.copy(selectedFlockIds = set).also { newState -> persistState(newState) }
        }
    }

    fun proceedToDraft() {
        _uiState.update { it.copy(currentStep = GoalWizardStep.DRAFT).also { newState -> persistState(newState) } }
        generatePlan()
    }

    fun generatePlan() {
        val state = _uiState.value
        val species = state.selectedSpecies ?: return
        
        // Synthesize a template from GoalSpecs
        val synthesizedTemplate = BreedingGoalTemplate(
            id = "synthesized_${java.util.UUID.randomUUID()}",
            title = "Strategic Project",
            description = "Custom strategic goal",
            mustHave = state.strategyConfig.goalSpecs.map { 
                com.example.hatchtracker.data.models.TraitTarget(it.traitKey, it.traitKey) 
            },
            diversityWeight = if (state.strategyConfig.strategyMode == StrategyMode.COMMERCIAL_PRODUCTION) 1.0 else 0.3
        )

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // OFF-THREAD GENERATION
                val draft = withContext(Dispatchers.Default) {
                    if (state.strategyConfig.startingSituation == StartingSituation.START_FROM_SCRATCH) {
                        val starterPlan = starterPlanService.generateStarterPlan(species, synthesizedTemplate)
                        starterPlan
                    } else {
                        if (!state.isPro) {
                             return@withContext null // Handled below with UI error
                        }
                        val request = StrategyRequest(
                            species = species,
                            mode = com.example.hatchtracker.data.models.ScenarioEntryMode.FORWARD,
                            selectedFlockIds = state.selectedFlockIds.toList(),
                            template = synthesizedTemplate,
                            constraints = PlanConstraints(maxGenerations = 10),
                            configuration = state.strategyConfig
                        )
                        val programs = breedingStrategyService.buildStrategy(request).first()
                        val best = programs.maxByOrNull { it.overallScore }
                        if (best != null) {
                            BreedingPlanDraft(
                                id = java.util.UUID.randomUUID().toString(),
                                planType = ProgramMode.FLOCK_BASED,
                                species = species,
                                goal = com.example.hatchtracker.data.models.BreedingTarget(
                                    requiredTraits = synthesizedTemplate.mustHave,
                                    preferredTraits = synthesizedTemplate.niceToHave,
                                    excludedTraits = synthesizedTemplate.avoid
                                ),
                                summaryRationale = best.summaryRationale,
                                steps = BreedingProgramConverter.convert(best).steps
                            )
                        } else null
                    }
                }

                if (draft == null) {
                    val errorMsg = if (state.strategyConfig.startingSituation != StartingSituation.START_FROM_SCRATCH && !state.isPro) {
                        "PRO_FEATURE_REQUIRED"
                    } else {
                        "NO_VIABLE_STRATEGY"
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                } else {
                    _uiState.update { it.copy(draft = draft, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun backStep() {
        _uiState.update { state ->
            val prev = when (state.currentStep) {
                GoalWizardStep.SPECIES -> GoalWizardStep.SPECIES
                GoalWizardStep.SITUATION -> GoalWizardStep.SPECIES
                GoalWizardStep.FLOCKS -> GoalWizardStep.SITUATION
                GoalWizardStep.TRAIT_DOMAINS -> if (state.strategyConfig.startingSituation == StartingSituation.START_FROM_SCRATCH) GoalWizardStep.SITUATION else GoalWizardStep.FLOCKS
                GoalWizardStep.TRAIT_SPECS -> GoalWizardStep.TRAIT_DOMAINS
                GoalWizardStep.STRATEGY_MODE -> GoalWizardStep.TRAIT_SPECS
                GoalWizardStep.ANALYSIS -> GoalWizardStep.STRATEGY_MODE
                GoalWizardStep.DRAFT -> GoalWizardStep.ANALYSIS
            }
            state.copy(currentStep = prev).also { newState -> persistState(newState) }
        }
    }
}
