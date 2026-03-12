package com.example.hatchtracker.feature.breeding.actionplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.BreedingProgram
import com.example.hatchtracker.data.models.AssetType
import com.example.hatchtracker.data.repository.BreedingProgramRepository
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.domain.breeding.ProgramLinkingService
import com.example.hatchtracker.domain.breeding.ProgramExecutionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.hatchtracker.data.models.*

data class GenerationFinancials(
    val generation: Int,
    val totalCostGross: Double = 0.0,
    val totalRevenueGross: Double = 0.0,
    val projectedCost: Double? = null,
    val projectedRevenue: Double? = null
)

@HiltViewModel
class BreedingProgramDetailViewModel @Inject constructor(
    private val planRepository: BreedingProgramRepository,
    private val financialRepository: FinancialRepository,
    private val linkingService: ProgramLinkingService,
    private val executionEngine: ProgramExecutionEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: String = savedStateHandle.get<String>("planId") ?: ""
    
    val dashboardState: StateFlow<ExecutionDashboardState?> = if (planId.isNotBlank()) {
        executionEngine.observeExecution(planId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        MutableStateFlow(null)
    }

    val plan: StateFlow<BreedingProgram?> = dashboardState.map { it?.program }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    private val _genFinancials = MutableStateFlow<Map<Int, GenerationFinancials>>(emptyMap())
    val genFinancials: StateFlow<Map<Int, GenerationFinancials>> = _genFinancials.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        observePlanForFinancials()
    }

    private fun observePlanForFinancials() {
        viewModelScope.launch {
            plan.collect { activePlan ->
                if (activePlan != null) {
                    loadFinancials(activePlan)
                }
            }
        }
    }

    private suspend fun loadFinancials(activePlan: BreedingProgram) {
        val genMap = mutableMapOf<Int, GenerationFinancials>()
        activePlan.steps.forEach { step ->
            val assets = activePlan.linkedAssets.filter { it.generationIndex == step.generation }
            
            var genCost = 0.0
            var genRevenue = 0.0
            
            assets.forEach { asset ->
                val ownerType = when (asset.type) {
                    AssetType.FLOCK -> "flock"
                    AssetType.FLOCKLET -> "flocklet"
                    AssetType.BIRD -> "bird"
                    AssetType.INCUBATION -> "incubation"
                }
                val stats = financialRepository.getAggregatedStats(ownerType, asset.refId, 0, System.currentTimeMillis()).first()
                genCost += stats.totalCost
                genRevenue += stats.totalRevenue
            }
            
            genMap[step.generation] = GenerationFinancials(
                generation = step.generation,
                totalCostGross = genCost,
                totalRevenueGross = genRevenue
            )
        }
        _genFinancials.value = genMap
    }
    
    fun toggleAction(stageId: String, actionId: String, done: Boolean) {
        viewModelScope.launch {
            executionEngine.toggleActionDone(planId, stageId, actionId, done)
        }
    }

    fun addExecutionNote(text: String, scope: NoteScope, stageId: String? = null) {
        viewModelScope.launch {
            val note = ExecutionNote(
                noteId = java.util.UUID.randomUUID().toString(),
                scope = scope,
                stageId = stageId,
                text = text
            )
            executionEngine.addNote(planId, note)
        }
    }

    fun setMergeMode(mode: com.example.hatchtracker.data.models.MergeMode) {
        viewModelScope.launch {
            linkingService.setMergeMode(planId, mode)
        }
    }

    fun setSelectedBirds(genIndex: Int, birdCloudIds: List<String>) {
        viewModelScope.launch {
            linkingService.setSelectedBirds(planId, genIndex, birdCloudIds)
        }
    }

    fun linkBirdToSlot(stepOrder: Int, slotRole: String, birdId: String, birdName: String) {
        val currentPlan = plan.value ?: return
        
        val updatedSteps = currentPlan.steps.map { step ->
            if (step.order == stepOrder) {
                val updatedSlots = step.requiredParents.map { slot ->
                    if (slot.role.name == slotRole) {
                         slot.copy(source = "BIRD:$birdId", displayName = birdName)
                    } else {
                        slot
                    }
                }
                step.copy(requiredParents = updatedSlots)
            } else {
                step
            }
        }
        
        val updatedPlan = currentPlan.copy(steps = updatedSteps, updatedAt = System.currentTimeMillis())
        
        viewModelScope.launch {
             planRepository.updatePlan(updatedPlan)
        }
    }
}
