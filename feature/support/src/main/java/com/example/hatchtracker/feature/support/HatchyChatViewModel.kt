package com.example.hatchtracker.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.hatchtracker.domain.hatchy.model.HatchyChatMessage
import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.routing.HatchyIntent
import com.example.hatchtracker.domain.hatchy.routing.IHatchyContextProvider
import com.example.hatchtracker.domain.hatchy.HatchyOrchestrator
import com.example.hatchtracker.domain.hatchy.routing.AnswerConfidence
import com.example.hatchtracker.domain.hatchy.routing.AnswerSource
import com.example.hatchtracker.domain.hatchy.routing.AnswerType
import com.example.hatchtracker.domain.hatchy.model.SenderType
import com.example.hatchtracker.domain.hatchy.model.FeedbackType
import com.example.hatchtracker.data.models.SupportTicket
import com.example.hatchtracker.data.models.TicketCategory
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.repository.SupportRepository
import com.example.hatchtracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HatchyChatViewModel @Inject constructor(
    private val subscriptionStateManager: SubscriptionStateManager,
    private val supportRepository: SupportRepository,
    private val userRepository: UserRepository,
    private val hatchyContextProvider: IHatchyContextProvider,
    private val orchestrator: HatchyOrchestrator,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HatchyChatUiState())
    val uiState: StateFlow<HatchyChatUiState> = _uiState.asStateFlow()

    private var processingJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            subscriptionStateManager.currentCapabilities.collect { caps ->
                _uiState.update { it.copy(isPro = caps.isSelectiveBreedingEnabled) }
            }
        }
        
        // Initial greeting
        addMessage(HatchyChatMessage(
            scenarioId = "general",
            text = "Hi, I’m Hatchy. I can help with breeding, incubation, nursery care, flock management, and using the app. Some unusual questions may still need a different phrasing.",
            sender = SenderType.HATCHY
        ))
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = HatchyChatMessage(
            scenarioId = "general",
            text = text,
            sender = SenderType.USER
        )
        addMessage(userMessage)
        
        processHatchyResponse(text)
    }

    fun submitFeedback(messageId: String, type: FeedbackType, comment: String? = null) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == messageId) msg.copy(feedback = type) else msg
            }
            state.copy(messages = updatedMessages)
        }

        if (type == FeedbackType.THUMBS_DOWN) {
            val message = _uiState.value.messages.find { it.id == messageId } ?: return
            viewModelScope.launch {
                val profile = userRepository.userProfile.value
                val diagnostics = SupportDiagnosticsBuilder.build(
                    appContext,
                    profile?.userId,
                    subscriptionStateManager.currentCapabilities.value.tier
                )
                val ticket = SupportTicket(
                    userId = profile?.userId ?: "anonymous",
                    userEmail = profile?.email ?: "",
                    category = "GENERAL",
                    type = "HATCHY_FEEDBACK",
                    categoryDetail = TicketCategory(
                        moduleId = "hatchy",
                        moduleName = "Hatchy AI Assistant",
                        featureId = "advice_feedback",
                        featureName = "Negative Advice Feedback"
                    ),
                    status = com.example.hatchtracker.data.models.TicketStatus.SUBMITTED,
                    subscriptionTierAtCreation = subscriptionStateManager.currentCapabilities.value.tier,
                    subject = "Negative Advice Feedback",
                    description = "Hatchy Advice: \"${message.text}\"\n\nUser Feedback: ${comment ?: "No comment provided."}",
                    appVersion = diagnostics.appVersionName,
                    deviceInfo = mapOf("model" to android.os.Build.MODEL),
                    diagnostics = diagnostics,
                    hatchyTriage = mapOf(
                        "classification" to "Hatchy Feedback",
                        "confidenceScore" to 0.4,
                        "suggestedResponse" to "Thanks for the feedback. We'll review and improve Hatchy's guidance.",
                        "disclaimer" to "Hatchy is an AI helper. Suggestions may be imperfect. A human will review your ticket."
                    )
                )
                supportRepository.submitTicket(ticket)
            }
        }
    }

    private fun addMessage(message: HatchyChatMessage) {
        _uiState.update { it.copy(messages = it.messages + message) }
    }

    private fun processHatchyResponse(userQuery: String) {
        // Cancel any pending response if user sends a new one
        processingJob?.cancel()
        
        processingJob = viewModelScope.launch {
            try {
                _uiState.update { state -> state.copy(isProcessing = true, thinkingLabel = "Hatchy is thinking...") }
                
                val context = hatchyContextProvider.context.value
                
                orchestrator.processQueryFlow(userQuery, context, context.localeTag).collect { event ->
                    when (event) {
                        is com.example.hatchtracker.domain.hatchy.routing.HatchyProcessEvent.Thinking -> {
                            _uiState.update { it.copy(thinkingLabel = event.label) }
                        }
                        is com.example.hatchtracker.domain.hatchy.routing.HatchyProcessEvent.Done -> {
                            addMessage(HatchyChatMessage(
                                scenarioId = "orchestrated",
                                text = event.answer.text,
                                sender = SenderType.HATCHY,
                                debugMetadata = if (_uiState.value.isPro) event.answer.debugMetadata else null
                            ))
                        }
                    }
                }
            } finally {
                _uiState.update { state -> state.copy(isProcessing = false, thinkingLabel = null) }
            }
        }
    }
}

@androidx.compose.runtime.Immutable
data class HatchyChatUiState(
    val messages: List<HatchyChatMessage> = emptyList(),
    val isProcessing: Boolean = false,
    val thinkingLabel: String? = null,
    val isPro: Boolean = false
)
