package com.example.hatchtracker.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.InboxNotification
import com.example.hatchtracker.data.repository.InboxNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val inboxRepository: InboxNotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<InboxNotification>> = inboxRepository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            inboxRepository.markAsRead(id)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            inboxRepository.delete(id)
        }
    }

    fun snooze(id: Long) {
        viewModelScope.launch {
            // Snooze for 24 hours
            val until = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
            inboxRepository.snooze(id, until)
        }
    }

    fun clearReadInfo() {
        viewModelScope.launch {
            inboxRepository.clearReadInfo()
        }
    }
}


