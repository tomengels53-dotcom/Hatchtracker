package com.example.hatchtracker.data.remote.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore-mapped chat message for tickets/{ticketId}/messages.
 */
data class ChatMessageDocument(
    @DocumentId
    val id: String = "",
    val ticketId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "user",
    val content: String = "",
    val isInternal: Boolean = false,
    val attachments: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val readBy: List<String> = emptyList()
)
