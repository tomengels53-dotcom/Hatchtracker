@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.example.hatchtracker.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.auth.SessionManager
import com.example.hatchtracker.data.firestore.FirestoreTicketType
import com.example.hatchtracker.data.models.SupportTicket
import com.example.hatchtracker.data.models.TicketStatus
import com.example.hatchtracker.data.repository.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TicketAction {
    APPROVE,
    RESOLVE,
    REJECT
}

@HiltViewModel
class AdminSupportViewModel @Inject constructor(
    private val supportRepository: SupportRepository,
    private val sessionManager: SessionManager,
    private val subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager
) : ViewModel() {

    // --- Action Tracking ---
    private val _currentAction = MutableStateFlow<TicketAction?>(null)
    val currentAction: StateFlow<TicketAction?> = _currentAction.asStateFlow()

    // --- List State ---
    // Guarded Flow: Only emit tickets if System Admin
    val allTickets = subscriptionStateManager.isAdmin
        .flatMapLatest { isAdmin ->
            if (isAdmin) {
                supportRepository.getAllTickets().map { list ->
                    list.sortedWith(
                        compareByDescending<SupportTicket> { it.priority }
                            .thenBy { it.createdAt?.time ?: 0L }
                    )
                }
            } else {
                flowOf(emptyList()) // Security Guard
            }
        }
        .catch { e ->
            // Log error and emit empty list to prevent crash
            android.util.Log.e("AdminSupportViewModel", "Error fetching tickets", e)
            emit(emptyList())
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // --- Detail State ---
    private val _selectedTicketId = MutableStateFlow<String?>(null)
    
    val selectedTicket = combine(
        _selectedTicketId,
        subscriptionStateManager.isAdmin
    ) { id, isAdmin ->
        Pair(id, isAdmin)
    }.flatMapLatest { (id, isAdmin) ->
        if (id == null || !isAdmin) flowOf(null)
        else supportRepository.getAllTickets()
            .map { list -> list.find { it.ticketId == id } }
            .catch { emit(null) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val internalNotes = _selectedTicketId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        // Assuming repository handles permission error or returns empty if rule fails
        // But we add check here too? The flow above guards 'selectedTicket', ensuring UI is blank.
        else supportRepository.getInternalNotes(id)
            .catch { emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Actions ---

    fun selectTicket(ticketId: String) {
        _selectedTicketId.value = ticketId
    }

    fun clearSelection() {
        _selectedTicketId.value = null
    }

    fun updateStatus(status: TicketStatus) = sendStatus(status, null)

    fun approveTicket() = sendStatus(TicketStatus.APPROVED, TicketAction.APPROVE)

    fun resolveTicket() = sendStatus(TicketStatus.RESOLVED, TicketAction.RESOLVE)

    fun rejectTicket() = sendStatus(TicketStatus.REJECTED, TicketAction.REJECT)

    private fun sendStatus(status: TicketStatus, action: TicketAction?) {
        val isAdmin = subscriptionStateManager.isAdmin.value
        if (!isAdmin) return

        val ticket = selectedTicket.value ?: return
        val user = sessionManager.getCurrentUser() ?: return

        viewModelScope.launch {
            _currentAction.value = action
            try {
                val changeRequest = ticket.changeRequest
                val shouldHandleCountryChange =
                    changeRequest?.type == FirestoreTicketType.COUNTRY_CHANGE &&
                        status in setOf(TicketStatus.RESOLVED, TicketStatus.REJECTED)

                if (shouldHandleCountryChange) {
                    // Start of refactor:
                    // We no longer perform client-side resolution of country changes.
                    // Admin should set status to APPROVED, and User must "Apply" it.
                    // If Admin force-sets RESOLVED/REJECTED, we just update the status.
                    // Logic falls through to standard updateTicketStatus.
                     supportRepository.updateTicketStatus(ticket.ticketId, status, user.uid)
                } else {
                    supportRepository.updateTicketStatus(ticket.ticketId, status, user.uid)
                }
            } finally {
                _currentAction.value = null
            }
        }
    }

    fun addNote(content: String) {
        val isAdmin = subscriptionStateManager.isAdmin.value
        if (!isAdmin) return // Strict Guard

        val ticket = selectedTicket.value ?: return
        val user = sessionManager.getCurrentUser() ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            supportRepository.addInternalNote(ticket.ticketId, content, user.uid)
        }
    }
}



