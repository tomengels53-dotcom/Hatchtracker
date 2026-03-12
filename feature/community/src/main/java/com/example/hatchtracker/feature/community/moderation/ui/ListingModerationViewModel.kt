package com.example.hatchtracker.feature.community.moderation.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.repository.ConfigRepository
import com.example.hatchtracker.domain.model.ListingModerationState
import com.example.hatchtracker.domain.model.MarketplaceListing
import com.example.hatchtracker.domain.model.ReportReasonCode
import com.example.hatchtracker.domain.model.ReportTargetType
import com.example.hatchtracker.domain.model.ModerationActionLog
import com.example.hatchtracker.data.repository.ModerationActionLogRepository
import com.example.hatchtracker.domain.repository.MarketplaceListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListingModerationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val marketplaceRepository: MarketplaceListingRepository,
    private val logRepository: ModerationActionLogRepository,
    private val subscriptionStateManager: SubscriptionStateManager,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val listingId: String = checkNotNull(savedStateHandle["listingId"])

    private val _uiState = MutableStateFlow(ListingModerationUiState())
    val uiState: StateFlow<ListingModerationUiState> = _uiState.asStateFlow()

    val canAccess: StateFlow<Boolean> = combine(
        subscriptionStateManager.isCommunityAdmin,
        subscriptionStateManager.isAdmin,
        configRepository.observeAppAccessConfig()
    ) { isModerator, isSysAdmin, config ->
        (isModerator || isSysAdmin) && config.communityConfig.marketplaceModerationEnabled
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadListing()
    }

    private fun loadListing() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            marketplaceRepository.getListing(listingId).collect { listing ->
                _uiState.update { it.copy(listing = listing, isLoading = false) }
            }
        }
    }

    fun updateModerationState(newState: ListingModerationState, note: String) {
        viewModelScope.launch {
            val moderatorId = "moderator"
            val moderatorRole = if (subscriptionStateManager.isAdmin.first()) "SystemAdmin" else "CommunityAdmin"

            marketplaceRepository.updateModerationState(listingId, newState)
            
            logRepository.logAction(ModerationActionLog(
                actorUserId = moderatorId,
                actorRole = moderatorRole,
                targetType = ReportTargetType.MARKETPLACE_LISTING,
                targetId = listingId,
                actionType = "LISTING_MODERATION_UPDATE: $newState",
                reasonCode = ReportReasonCode.OTHER,
                moderatorNote = note
            ))
        }
    }
}

data class ListingModerationUiState(
    val listing: MarketplaceListing? = null,
    val isLoading: Boolean = false
)
