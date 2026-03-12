package com.example.hatchtracker.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.auth.SessionManager
import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.domain.model.UserProfile
import com.example.hatchtracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.hatchtracker.data.service.EquipmentAnalyticsService
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val equipmentRepository: com.example.hatchtracker.data.repository.EquipmentRepository,
    private val deviceCapacityManager: com.example.hatchtracker.domain.breeding.DeviceCapacityManager,
    private val subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager,
    private val supportRepository: com.example.hatchtracker.data.repository.SupportRepository,
    private val functionsRepository: com.example.hatchtracker.data.repository.FunctionsRepository,
    private val localeManager: com.example.hatchtracker.common.localization.LocaleManager,
    private val languagePreferences: com.example.hatchtracker.common.localization.LanguagePreferences,
    private val analyticsService: EquipmentAnalyticsService,
    private val birdLifecycleService: com.example.hatchtracker.data.service.BirdLifecycleService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val profileFlow = sessionManager.sessionState
        .flatMapLatest { state ->
            val uid = (state as? com.example.hatchtracker.auth.SessionState.Authenticated)?.user?.uid
            if (uid != null) userRepository.getProfileFlow(uid) else flowOf(null)
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    private val ticketsFlow = sessionManager.sessionState
        .flatMapLatest { state ->
            val uid = (state as? com.example.hatchtracker.auth.SessionState.Authenticated)?.user?.uid
            if (uid != null) supportRepository.getTicketsForUser(uid) else flowOf(emptyList())
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    init {
        observeState()
    }

    private fun observeState() {
        val devicesFlow = equipmentRepository.getUserEquipment()
        val capacityFlow = deviceCapacityManager.getCapacityForDevices(devicesFlow)

        // Collect analytics summaries for all devices
        capacityFlow.flatMapLatest { capacities ->
            val analyticsFlows = capacities.map { cap ->
                analyticsService.getMaintenanceFrequency(cap.device.id).combine(
                    analyticsService.getTotalMaintenanceCost(cap.device.id)
                ) { freq, cost ->
                    cap.device.id to ProfileUiState.DeviceAnalyticsSummary(freq, cost)
                }
            }
            if (analyticsFlows.isEmpty()) flowOf(emptyMap<String, ProfileUiState.DeviceAnalyticsSummary>())
            else combine(analyticsFlows) { it.toMap() }
        }.onEach { analyticsMap ->
            val current = _uiState.value as? ProfileUiState.Success
            if (current != null) {
                _uiState.value = current.copy(deviceAnalytics = analyticsMap)
            }
        }.launchIn(viewModelScope)

        combine(
            profileFlow, 
            capacityFlow,
            subscriptionStateManager.currentCapabilities,
            ticketsFlow,
            subscriptionStateManager.lastPlaySyncEpochMs
        ) { profile, devices, caps, tickets, lastPlaySyncEpochMs ->
            if (profile != null) {
                val pendingChange = tickets.find { ticket ->
                    ticket.status == com.example.hatchtracker.data.models.TicketStatus.APPROVED &&
                    ticket.changeRequest?.type == "COUNTRY_CHANGE" &&
                    ticket.changeRequest?.newValue != null
                }
                val current = _uiState.value as? ProfileUiState.Success
                ProfileUiState.Success(
                    profile = profile.copy(email = sessionManager.getCurrentUser()?.email ?: ""),
                    devices = devices,
                    deviceAnalytics = current?.deviceAnalytics ?: emptyMap(),
                    capabilities = caps,
                    lastPlaySyncEpochMs = lastPlaySyncEpochMs,
                    pendingCountryChangeTicket = pendingChange,
                    isUpdating = current?.isUpdating ?: false,
                    message = current?.message,
                    error = current?.error
                )
            } else {
                if (_uiState.value !is ProfileUiState.Loading && sessionManager.getCurrentUser() != null) {
                     userRepository.ensureProfileFromAuth(sessionManager.getCurrentUser()!!)
                }
                ProfileUiState.Loading 
            }
        }
        .onEach { state ->
             _uiState.value = state
        }
        .catch { e ->
             _uiState.value = ProfileUiState.Error(e.message ?: "Unknown error")
        }
        .launchIn(viewModelScope)
    }

    fun updateDisplayName(newName: String) {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = currentState.copy(isUpdating = true)
            val result = userRepository.updateProfileInfo(
                userId = currentState.profile.userId,
                displayName = newName,
                profilePictureUrl = currentState.profile.profilePictureUrl
            )
            if (result.isSuccess) {
                _uiState.value = (_uiState.value as ProfileUiState.Success).copy(
                    isUpdating = false,
                    message = UiText.StringResource(com.example.hatchtracker.feature.profile.R.string.profile_msg_updated)
                )
            } else {
                val errorMsg = com.example.hatchtracker.core.common.FirestoreErrorMapper.userMessage(result.exceptionOrNull())
                _uiState.value = currentState.copy(isUpdating = false, error = errorMsg)
            }
        }
    }

    fun changePassword(current: String, new: String) {
        viewModelScope.launch {
            _uiState.value = (_uiState.value as? ProfileUiState.Success)?.copy(isUpdating = true) ?: _uiState.value
            val result = sessionManager.updatePassword(current, new)
            if (result.isSuccess) {
                _uiState.value = (_uiState.value as ProfileUiState.Success).copy(
                    isUpdating = false,
                    message = UiText.StringResource(com.example.hatchtracker.feature.profile.R.string.profile_msg_password_changed)
                )
            } else {
                 val errorMsg = com.example.hatchtracker.core.common.FirestoreErrorMapper.userMessage(result.exceptionOrNull())
                _uiState.value = (_uiState.value as ProfileUiState.Success).copy(isUpdating = false, error = errorMsg)
            }
        }
    }

    fun applyCountryChange(ticket: com.example.hatchtracker.data.models.SupportTicket) {
        val changeRequest = ticket.changeRequest ?: return
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isUpdating = true)
             val updateResult = supportRepository.consumeCountryChangeTicket(
                ticketId = ticket.ticketId,
                newCountry = changeRequest.newValue,
                newCurrency = "" 
            )

            if (updateResult.isSuccess) {
                _uiState.value = (_uiState.value as ProfileUiState.Success).copy(
                        isUpdating = false, 
                        message = UiText.StringResource(
                            com.example.hatchtracker.feature.profile.R.string.profile_msg_country_updated,
                            changeRequest.newValue
                        ),
                        pendingCountryChangeTicket = null
                )
            } else {
                 val errorMsg = com.example.hatchtracker.core.common.FirestoreErrorMapper.userMessage(updateResult.exceptionOrNull())
                _uiState.value = currentState.copy(isUpdating = false, error = errorMsg)
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val currentState = _uiState.value as? ProfileUiState.Success ?: return@launch
            _uiState.value = currentState.copy(isUpdating = true)

            val result = functionsRepository.deleteAccount("DELETE")
            
            if (result.isSuccess) {
                sessionManager.signOut()
                _uiState.value = ProfileUiState.AccountDeleted
            } else {
                 val errorMsg = com.example.hatchtracker.core.common.FirestoreErrorMapper.userMessage(result.exceptionOrNull())
                _uiState.value = currentState.copy(
                    isUpdating = false, 
                    error = errorMsg
                )
            }
        }
    }

    fun clearMessage() {
        (_uiState.value as? ProfileUiState.Success)?.let {
            _uiState.value = it.copy(message = null, error = null)
        }
    }

    fun updateLanguage(tag: String) {
        val currentState = _uiState.value as? ProfileUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = currentState.copy(isUpdating = true)
            val result = userRepository.updateLanguage(currentState.profile.userId, tag)
            if (result.isSuccess) {
                languagePreferences.setLanguageTag(tag)
                localeManager.applyLanguage(tag)
                _uiState.value = currentState.copy(
                    isUpdating = false,
                    message = UiText.StringResource(com.example.hatchtracker.feature.profile.R.string.profile_msg_updated)
                )
            } else {
                val errorMsg = com.example.hatchtracker.core.common.FirestoreErrorMapper.userMessage(result.exceptionOrNull())
                _uiState.value = currentState.copy(isUpdating = false, error = errorMsg)
            }
        }
    }

    // Phase 4 - Maintenance & Analytics
    fun getMaintenanceLogs(deviceId: String) = equipmentRepository.getMaintenanceLogs(deviceId)
    
    fun getMaintenanceFrequency(deviceId: String) = analyticsService.getMaintenanceFrequency(deviceId)
    
    fun getTotalMaintenanceCost(deviceId: String) = analyticsService.getTotalMaintenanceCost(deviceId)

    fun addMaintenanceLog(log: com.example.hatchtracker.model.EquipmentMaintenanceLog) {
        viewModelScope.launch {
            equipmentRepository.addMaintenanceLog(log)
            // Log global domain event
            birdLifecycleService.recordMaintenance(
                deviceId = log.equipmentId,
                type = log.type.name,
                notes = log.description
            )
        }
    }

    fun deleteMaintenanceLog(deviceId: String, logId: String) {
        viewModelScope.launch {
            equipmentRepository.deleteMaintenanceLog(deviceId, logId)
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: UserProfile,
        @Deprecated("Decentralized to Feature Hubs. Read-only legacy support in Phase 1.")
        val devices: List<com.example.hatchtracker.domain.breeding.DeviceCapacity> = emptyList(),
        val deviceAnalytics: Map<String, DeviceAnalyticsSummary> = emptyMap(),
        val capabilities: com.example.hatchtracker.billing.SubscriptionCapabilities =
            com.example.hatchtracker.billing.SubscriptionCapabilities.getForTier(
                com.example.hatchtracker.data.models.SubscriptionTier.FREE
            ),
        val lastPlaySyncEpochMs: Long = 0L,
        val pendingCountryChangeTicket: com.example.hatchtracker.data.models.SupportTicket? = null,
        val isUpdating: Boolean = false,
        val message: UiText? = null,
        val error: UiText? = null
    ) : ProfileUiState()
    
    data class DeviceAnalyticsSummary(
        val maintenanceFrequency: Double? = null,
        val totalMaintenanceCost: Double = 0.0
    )
    data class Error(val message: String) : ProfileUiState()
    object AccountDeleted : ProfileUiState()
}
