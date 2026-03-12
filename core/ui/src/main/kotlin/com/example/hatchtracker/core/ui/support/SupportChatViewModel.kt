package com.example.hatchtracker.core.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.auth.SessionManager
import com.example.hatchtracker.data.models.ChatMessage
import com.example.hatchtracker.data.repository.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportChatViewModel @Inject constructor(
    private val repository: SupportRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentTicketId: String? = null

    fun loadMessages(ticketId: String) {
        currentTicketId = ticketId
        viewModelScope.launch {
            repository.getMessages(ticketId)
                .catch { e: Throwable -> 
                    // Handle error (e.g. log it)
                }
                .collect { list: List<ChatMessage> ->
                    _messages.value = list
                }
        }
    }

    fun sendMessage(content: String, isInternal: Boolean = false) {
        val ticketId = currentTicketId ?: return
        val user = sessionManager.getCurrentUser() ?: return
        if (content.isBlank()) return

        val isAdmin = com.example.hatchtracker.auth.UserAuthManager.isSystemAdmin.value
        
        // Determine role
        val role = if (isAdmin) "admin" else "user"
        // If user is admin but wants to reply publicly, sending role is still admin
        
        val message = ChatMessage(
            ticketId = ticketId,
            senderId = user.uid,
            senderName = user.displayName ?: "User", // TODO: Get better name source
            senderRole = role,
            content = content,
            isInternal = isInternal
        )

        viewModelScope.launch {
            repository.sendMessage(ticketId, message)
        }
    }
}




