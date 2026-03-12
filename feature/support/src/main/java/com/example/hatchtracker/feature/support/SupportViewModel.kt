@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.example.hatchtracker.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.hatchtracker.auth.SessionManager
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.models.ChatMessage
import com.example.hatchtracker.data.models.SupportFeature
import com.example.hatchtracker.data.models.SupportModule
import com.example.hatchtracker.data.models.SupportDiagnostics
import com.example.hatchtracker.data.models.SupportTicket
import com.example.hatchtracker.data.firestore.FirestoreTicketCategory
import com.example.hatchtracker.data.firestore.FirestoreTicketType
import com.example.hatchtracker.data.repository.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    val supportRepository: SupportRepository,
    val sessionManager: com.example.hatchtracker.auth.SessionManager,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    private val subscriptionStateManager: SubscriptionStateManager,
    @param:ApplicationContext private val appContext: Context,
    networkMonitor: com.example.hatchtracker.core.common.NetworkMonitor
) : ViewModel() {

    val userProfile = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAdmin = subscriptionStateManager.isAdmin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    val isOnline = networkMonitor.isOnline
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true
        )

    val userTickets = sessionManager.sessionState
        .flatMapLatest { state ->
            if (state is com.example.hatchtracker.auth.SessionState.Authenticated) {
                supportRepository.getTicketsForUser(state.user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private var ticketMessagesJob: Job? = null
    private var currentTicketWatching: String? = null
    private val _ticketMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val ticketMessages: StateFlow<List<ChatMessage>> = _ticketMessages.asStateFlow()

    init {
        _uiState.update { it.copy(modules = supportRepository.getSupportModules()) }
    }

    fun onModuleSelected(module: SupportModule) {
        _uiState.update {
            it.copy(
                selectedModule = module,
                selectedFeature = null,
                features = supportRepository.getSupportFeatures(module.id)
            )
        }
    }

    fun onFeatureSelected(feature: SupportFeature) {
        _uiState.update { it.copy(selectedFeature = feature) }
    }

    fun onMessageChanged(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun onRequestedCountryChanged(countryCode: String) {
        _uiState.update { it.copy(requestedCountry = countryCode) }
    }

    fun onAcknowledgementChanged(acknowledged: Boolean) {
        _uiState.update { it.copy(isAcknowledged = acknowledged) }
    }

    fun submitTicket() {
        val user = sessionManager.getCurrentUser() ?: return
        val current = _uiState.value
        if (current.selectedModule == null || current.selectedFeature == null || current.message.isBlank()) return

        // Abuse Prevention: Block duplicate country change requests
        if (current.selectedFeature.id == "change_country") {
            val existing = userTickets.value.any {
                val isCountryChange = it.categoryDetail.featureId == "change_country" ||
                    it.type == FirestoreTicketType.COUNTRY_CHANGE ||
                    it.changeRequest?.type == FirestoreTicketType.COUNTRY_CHANGE ||
                    it.changeRequest?.type == "country"
                isCountryChange &&
                it.status in setOf(
                    com.example.hatchtracker.data.models.TicketStatus.SUBMITTED,
                    com.example.hatchtracker.data.models.TicketStatus.IN_REVIEW,
                    com.example.hatchtracker.data.models.TicketStatus.APPROVED,
                    com.example.hatchtracker.data.models.TicketStatus.OPEN,
                    com.example.hatchtracker.data.models.TicketStatus.IN_PROGRESS,
                    com.example.hatchtracker.data.models.TicketStatus.WAITING_FOR_USER
                )
            }
            if (existing) {
                _uiState.update { it.copy(error = "You already have a pending country change request.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val diagnostics = buildDiagnostics(user.uid)
            val hatchyTriage = buildHatchyTriage(current)
            val ticketCategory = com.example.hatchtracker.data.models.TicketCategory(
                moduleId = current.selectedModule.id,
                moduleName = resolveModuleName(current.selectedModule.id, current.selectedModule.name),
                featureId = current.selectedFeature.id,
                featureName = resolveFeatureName(current.selectedFeature.id, current.selectedFeature.name)
            )
            val categoryKey = when (current.selectedModule.id) {
                "user_profile" -> FirestoreTicketCategory.PROFILE
                "localization" -> FirestoreTicketCategory.LOCALIZATION
                else -> current.selectedModule.id.uppercase()
            }
            val typeKey = when (current.selectedFeature.id) {
                "change_country" -> FirestoreTicketType.COUNTRY_CHANGE
                "translation_error" -> FirestoreTicketType.TRANSLATION_ERROR
                "missing_translation" -> FirestoreTicketType.MISSING_TRANSLATION
                "language_switch_issue" -> FirestoreTicketType.TRANSLATION_ERROR
                else -> current.selectedFeature.id.uppercase()
            }
            val ticket = SupportTicket(
                userId = user.uid,
                userEmail = user.email ?: "",
                category = categoryKey,
                type = typeKey,
                categoryDetail = ticketCategory,
                status = com.example.hatchtracker.data.models.TicketStatus.SUBMITTED,
                subscriptionTierAtCreation = subscriptionStateManager.currentCapabilities.value.tier,
                subject = if (current.selectedFeature.id == "change_country") {
                    appContext.getString(R.string.country_change_title)
                } else {
                    appContext.getString(
                        R.string.support_issue_subject_format,
                        resolveFeatureName(current.selectedFeature.id, current.selectedFeature.name)
                    )
                },
                description = current.message,
                changeRequest = if (current.selectedFeature.id == "change_country") {
                    com.example.hatchtracker.data.models.ChangeRequest(
                        type = FirestoreTicketType.COUNTRY_CHANGE,
                        oldValue = userProfile.value?.countryCode ?: "",
                        newValue = current.requestedCountry,
                        reason = current.message,
                        userAcknowledgement = current.isAcknowledged
                    )
                } else null,
                appVersion = diagnostics.appVersionName,
                deviceInfo = mapOf(
                    "manufacturer" to android.os.Build.MANUFACTURER,
                    "model" to android.os.Build.MODEL,
                    "os_version" to android.os.Build.VERSION.RELEASE
                ),
                diagnostics = diagnostics,
                hatchyTriage = hatchyTriage
            )
            
            val result = supportRepository.submitTicket(ticket)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isSubmitting = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun buildDiagnosticsForClipboard(): SupportDiagnostics? {
        val userId = sessionManager.getCurrentUser()?.uid
        return buildDiagnostics(userId)
    }

    fun reset() {
        _uiState.update { SupportUiState(modules = supportRepository.getSupportModules()) }
    }

    fun watchMessagesForTicket(ticketId: String) {
        if (ticketId == currentTicketWatching) return
        ticketMessagesJob?.cancel()
        ticketMessagesJob = supportRepository.getMessages(ticketId)
            .onEach { list -> _ticketMessages.value = list.filter { !it.isInternal } }
            .catch { /* ignore errors for now */ }
            .launchIn(viewModelScope)
        currentTicketWatching = ticketId
    }

    fun clearTicketMessages() {
        ticketMessagesJob?.cancel()
        ticketMessagesJob = null
        currentTicketWatching = null
        _ticketMessages.value = emptyList()
    }

    fun sendTicketMessage(ticketId: String, content: String) {
        if (content.isBlank()) return
        val user = sessionManager.getCurrentUser()
        val message = ChatMessage(
            ticketId = ticketId,
            senderId = user?.uid ?: "anonymous",
            senderName = user?.displayName ?: user?.email ?: "You",
            senderRole = "user",
            content = content,
            isInternal = false,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            supportRepository.sendMessage(ticketId, message)
        }
    }

    private fun buildDiagnostics(userId: String?): SupportDiagnostics {
        val tier = subscriptionStateManager.currentCapabilities.value.tier
        return SupportDiagnosticsBuilder.build(appContext, userId, tier)
    }

    private fun buildHatchyTriage(current: SupportUiState): Map<String, Any> {
        val moduleName = current.selectedModule?.let { resolveModuleName(it.id, it.name) }
            ?: appContext.getString(R.string.support_module_other)
        val featureName = current.selectedFeature?.let { resolveFeatureName(it.id, it.name) }
            ?: appContext.getString(R.string.support_feature_general_issue)
        val classification = "$moduleName > $featureName"
        val suggested = "Hatchy says: thanks for the details. First, try restarting the app and redoing the last step. " +
            "If it still acts up, share any error message or a quick screenshot."
        val nextSteps = listOf(
            "Confirm the exact steps that lead to the issue.",
            "Restart the app and retry the action.",
            "If it keeps happening, include a screenshot or error message."
        )
        return mapOf(
            "classification" to classification,
            "confidenceScore" to 0.55,
            "suggestedResponse" to suggested,
            "nextSteps" to nextSteps,
            "disclaimer" to "Hatchy is an AI helper. Suggestions may be imperfect. A human will review your ticket."
        )
    }

    private fun resolveModuleName(moduleId: String, fallback: String): String {
        return when (moduleId) {
            "flock" -> appContext.getString(R.string.support_module_flock)
            "incubation" -> appContext.getString(R.string.support_module_incubation)
            "breeding" -> appContext.getString(R.string.support_module_breeding)
            "nursery" -> appContext.getString(R.string.support_module_nursery)
            "financial" -> appContext.getString(R.string.support_module_financial)
            "user_profile" -> appContext.getString(R.string.support_module_profile)
            "localization" -> appContext.getString(R.string.support_module_localization)
            "other" -> appContext.getString(R.string.support_module_other)
            else -> fallback
        }
    }

    private fun resolveFeatureName(featureId: String, fallback: String): String {
        return when (featureId) {
            "add_bird" -> appContext.getString(R.string.support_feature_adding_birds)
            "flock_edit" -> appContext.getString(R.string.support_feature_editing_flocks)
            "inventory" -> appContext.getString(R.string.support_feature_bird_inventory)
            "start_hatch" -> appContext.getString(R.string.support_feature_starting_hatch)
            "candling" -> appContext.getString(R.string.support_feature_candling_results)
            "hatch_outcome" -> appContext.getString(R.string.support_feature_hatch_statistics)
            "pairing" -> appContext.getString(R.string.support_feature_creating_pairs)
            "recommendation" -> appContext.getString(R.string.support_feature_ai_recommendations)
            "compatibility" -> appContext.getString(R.string.support_feature_genetic_compatibility)
            "flocklet_stats" -> appContext.getString(R.string.support_feature_updating_chick_stats)
            "brooder_temp" -> appContext.getString(R.string.support_feature_temperature_tracking)
            "move_to_flock" -> appContext.getString(R.string.support_feature_moving_to_adult_flock)
            "sales" -> appContext.getString(R.string.support_feature_sales_tracking)
            "expenses" -> appContext.getString(R.string.support_feature_expense_logging)
            "summary" -> appContext.getString(R.string.support_feature_financial_overview)
            "change_country" -> appContext.getString(R.string.support_feature_country_change)
            "data_correction" -> appContext.getString(R.string.support_feature_account_data_correction)
            "identity_mismatch" -> appContext.getString(R.string.support_feature_identity_mismatch)
            "translation_error" -> appContext.getString(R.string.support_feature_translation_error)
            "missing_translation" -> appContext.getString(R.string.support_feature_missing_translation)
            "language_switch_issue" -> appContext.getString(R.string.support_feature_language_switch_issue)
            "general" -> appContext.getString(R.string.support_feature_general_issue)
            "bug" -> appContext.getString(R.string.support_feature_report_bug)
            "suggestion" -> appContext.getString(R.string.support_feature_suggestion)
            else -> fallback
        }
    }
}

@androidx.compose.runtime.Immutable
data class SupportUiState(
    val modules: List<SupportModule> = emptyList(),
    val features: List<SupportFeature> = emptyList(),
    val selectedModule: SupportModule? = null,
    val selectedFeature: SupportFeature? = null,
    val message: String = "",
    val requestedCountry: String = "",
    val isAcknowledged: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)




