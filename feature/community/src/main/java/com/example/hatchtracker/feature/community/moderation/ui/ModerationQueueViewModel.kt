package com.example.hatchtracker.feature.community.moderation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.models.AppAccessConfig
import com.example.hatchtracker.data.repository.ConfigRepository
import com.example.hatchtracker.data.repository.ModerationQueueRepository
import com.example.hatchtracker.domain.model.ModerationQueueEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModerationQueueViewModel @Inject constructor(
    private val queueRepository: ModerationQueueRepository,
    private val subscriptionStateManager: SubscriptionStateManager,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModerationQueueUiState())
    val uiState: StateFlow<ModerationQueueUiState> = _uiState.asStateFlow()

    /**
     * Moderation Tooling Access: CommunityAdmin or SystemAdmin only.
     */
    val canAccess: StateFlow<Boolean> = combine(
        subscriptionStateManager.isCommunityAdmin,
        subscriptionStateManager.isAdmin, // SystemAdmin
        configRepository.observeAppAccessConfig()
    ) { isModerator, isSysAdmin, config ->
        (isModerator || isSysAdmin) && config.communityConfig.moderationEnabled
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadQueue()
    }

    fun loadQueue() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val entries = queueRepository.getQueue()
            _uiState.update { it.copy(entries = entries, isLoading = false) }
        }
    }
}

data class ModerationQueueUiState(
    val entries: List<ModerationQueueEntry> = emptyList(),
    val isLoading: Boolean = false
)
