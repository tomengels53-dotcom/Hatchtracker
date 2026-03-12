package com.example.hatchtracker.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.model.AdminAuditLog
import com.example.hatchtracker.model.AuditActionType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class AdminAuditLogUiState(
    val logs: List<AdminAuditLog> = emptyList(),
    val filteredLogs: List<AdminAuditLog> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedActionFilter: AuditActionType? = null,
    val searchQuery: String = "" // Searches collection/document ID/email
)

@HiltViewModel
class AdminAuditLogViewModel @Inject constructor() : ViewModel() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val _uiState = MutableStateFlow(AdminAuditLogUiState())
    val uiState: StateFlow<AdminAuditLogUiState> = _uiState.asStateFlow()

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        observeLogs()
    }

    private fun observeLogs() {
        _uiState.update { it.copy(isLoading = true) }
        
        listenerRegistration = db.collection("adminAuditLogs")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100) // Safety limit
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                    return@addSnapshotListener
                }

                try {
                    val logs = snapshot?.toObjects(AdminAuditLog::class.java) ?: emptyList()
                    _uiState.update { 
                        it.copy(
                            logs = logs,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AdminAuditLogViewModel", "Error deserializing logs", e)
                    _uiState.update { it.copy(error = "Data error: ${e.message}", isLoading = false) }
                }
                applyFilters()
            }
    }

    fun updateActionFilter(action: AuditActionType?) {
        _uiState.update { it.copy(selectedActionFilter = action) }
        applyFilters()
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.logs.filter { log ->
            val matchesAction = state.selectedActionFilter == null || log.actionType == state.selectedActionFilter
            
            val query = state.searchQuery.lowercase()
            val matchesSearch = if (query.isBlank()) true else {
                log.adminEmail.lowercase().contains(query) ||
                log.targetCollection.lowercase().contains(query) ||
                log.targetDocumentId.lowercase().contains(query) ||
                (log.reason?.lowercase()?.contains(query) == true)
            }
            
            matchesAction && matchesSearch
        }
        _uiState.update { it.copy(filteredLogs = filtered) }
    }
}


