package com.example.hatchtracker.feature.nursery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Flocklet
import com.example.hatchtracker.data.repository.NurseryRepository
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.data.models.FinancialSummary
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FlockletDetailViewModel @Inject constructor(
    private val nurseryRepository: NurseryRepository,
    private val financialRepository: FinancialRepository,
    private val subscriptionStateManager: SubscriptionStateManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _flockletId = MutableStateFlow<Long?>(null)
    
    val flocklet: StateFlow<Flocklet?> = _flockletId
        .filterNotNull()
        .flatMapLatest { id ->
            flow { emit(nurseryRepository.getFlockletById(id)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val financialSummary: StateFlow<FinancialSummary?> = flocklet
        .filterNotNull()
        .flatMapLatest { f ->
            financialRepository.observeFinancialSummary(f.syncId, "flocklet")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val capabilities = subscriptionStateManager.currentCapabilities
    val isAdmin = subscriptionStateManager.isAdmin
    val isDeveloper = subscriptionStateManager.isDeveloper
    
    val currencyCode: StateFlow<String> = userRepository.userProfile
        .map { it?.currencyCode ?: "USD" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    fun setFlockletId(id: Long) {
        _flockletId.value = id
    }
}
