package com.example.hatchtracker.feature.breeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.auth.SessionManager
import com.example.hatchtracker.data.repository.BreedingProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class BreedingHomeViewModel @Inject constructor(
    private val planRepository: BreedingProgramRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId = sessionManager.getCurrentUser()?.uid ?: ""

    val planCount = if (userId.isNotEmpty()) {
        planRepository.observePlans(userId)
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    } else {
        MutableStateFlow(0)
    }
}
