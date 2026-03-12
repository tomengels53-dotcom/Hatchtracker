package com.example.hatchtracker.feature.community.moderation.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.repository.ConfigRepository
import com.example.hatchtracker.data.repository.UserSafetyRepository
import com.example.hatchtracker.domain.model.ReportReasonCode
import com.example.hatchtracker.domain.model.UserSafetyState
import com.example.hatchtracker.domain.service.ModerationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSafetyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val safetyRepository: UserSafetyRepository,
    private val moderationService: ModerationService,
    private val subscriptionStateManager: SubscriptionStateManager,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(UserSafetyUiState())
    val uiState: StateFlow<UserSafetyUiState> = _uiState.asStateFlow()

    val canAccess: StateFlow<Boolean> = combine(
        subscriptionStateManager.isCommunityAdmin,
        subscriptionStateManager.isAdmin,
        configRepository.observeAppAccessConfig()
    ) { isModerator, isSysAdmin, config ->
        (isModerator || isSysAdmin) && config.communityConfig.moderationEnabled
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadSafetyState()
    }

    private fun loadSafetyState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = safetyRepository.getSafetyState(userId)
            _uiState.update { it.copy(safetyState = state, isLoading = false) }
        }
    }

    fun applyRestriction(type: String, durationMins: Long, reason: ReportReasonCode, note: String) {
        viewModelScope.launch {
            val moderatorId = "moderator"
            val moderatorRole = if (subscriptionStateManager.isAdmin.first()) "SystemAdmin" else "CommunityAdmin"

            moderationService.applyRestriction(
                userId = userId,
                moderatorId = moderatorId,
                moderatorRole = moderatorRole,
                restrictionType = type,
                durationMs = durationMins * 60 * 1000,
                reason = reason,
                note = note
            )
            loadSafetyState()
        }
    }
}

data class UserSafetyUiState(
    val safetyState: UserSafetyState? = null,
    val isLoading: Boolean = false
)
