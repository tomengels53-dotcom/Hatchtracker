package com.example.hatchtracker.feature.breeding.actionplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.auth.SessionManager
import com.example.hatchtracker.data.models.BreedingProgramStatus
import com.example.hatchtracker.data.repository.BreedingProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BreedingProgramHubViewModel @Inject constructor(
    private val planRepository: BreedingProgramRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getCurrentUser()?.uid ?: ""

    val plans = if (userId.isNotEmpty()) {
        planRepository.observePlans(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            planRepository.deletePlan(planId)
        }
    }
    
    fun archivePlan(planId: String) {
        viewModelScope.launch {
            val result = planRepository.getPlan(planId)
            result.onSuccess { plan ->
                 planRepository.updatePlan(plan.copy(status = BreedingProgramStatus.ARCHIVED))
            }
        }
    }
}
