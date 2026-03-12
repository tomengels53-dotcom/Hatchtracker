package com.example.hatchtracker.feature.incubation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.repository.IncubationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.repository.UserRepository

@HiltViewModel
class TroubleshootingViewModel @Inject constructor(
    private val repository: IncubationRepository,
    private val userRepository: UserRepository,
    private val subscriptionManager: SubscriptionStateManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val incubationId: Long = savedStateHandle["incubationId"] ?: -1L

    private val _incubation = MutableStateFlow<Incubation?>(null)
    val incubation: StateFlow<Incubation?> = _incubation.asStateFlow()

    val userProfile = userRepository.userProfile
    val currentCapabilities = subscriptionManager.currentCapabilities

    init {
        loadIncubation()
    }

    private fun loadIncubation() {
        if (incubationId == -1L) return
        viewModelScope.launch {
            _incubation.value = repository.getIncubationById(incubationId)
        }
    }
}
