package com.example.hatchtracker.feature.community.moderation.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.repository.ConfigRepository
import com.example.hatchtracker.data.repository.ReportRepository
import com.example.hatchtracker.domain.model.CommunityReport
import com.example.hatchtracker.domain.model.ReportReasonCode
import com.example.hatchtracker.domain.model.ReportStatus
import com.example.hatchtracker.domain.service.ModerationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reportRepository: ReportRepository,
    private val moderationService: ModerationService,
    private val subscriptionStateManager: SubscriptionStateManager,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val reportId: String = checkNotNull(savedStateHandle["reportId"])
    
    private val _uiState = MutableStateFlow(ReportDetailUiState())
    val uiState: StateFlow<ReportDetailUiState> = _uiState.asStateFlow()

    val canAccess: StateFlow<Boolean> = combine(
        subscriptionStateManager.isCommunityAdmin,
        subscriptionStateManager.isAdmin,
        configRepository.observeAppAccessConfig()
    ) { isModerator, isSysAdmin, config ->
        (isModerator || isSysAdmin) && config.communityConfig.moderationEnabled
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadReport()
    }

    private fun loadReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val report = reportRepository.getReport(reportId)
            _uiState.update { it.copy(report = report, isLoading = false) }
        }
    }

    fun resolveReport(status: ReportStatus, note: String, isActionTaken: Boolean) {
        viewModelScope.launch {
            val moderatorId = "moderator"
            val moderatorRole = if (subscriptionStateManager.isAdmin.first()) "SystemAdmin" else "CommunityAdmin"
            
            moderationService.resolveReport(
                reportId = reportId,
                moderatorId = moderatorId,
                moderatorRole = moderatorRole,
                status = status,
                resolutionNote = note,
                isActionTaken = isActionTaken
            )
            loadReport()
        }
    }

    fun issueStrike(reason: ReportReasonCode, note: String) {
        viewModelScope.launch {
            val userId = _uiState.value.report?.reportedUserId ?: return@launch
            val moderatorId = "moderator"
            val moderatorRole = if (subscriptionStateManager.isAdmin.first()) "SystemAdmin" else "CommunityAdmin"

            moderationService.issueStrike(userId, moderatorId, moderatorRole, reason, note)
            resolveReport(ReportStatus.RESOLVED_ACTION_TAKEN, "Strike issued: $note", true)
        }
    }
}

data class ReportDetailUiState(
    val report: CommunityReport? = null,
    val isLoading: Boolean = false
)
