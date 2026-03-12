package com.example.hatchtracker.data.models


/**
 * Represents a single message in the support ticket chat.
 * Collection: tickets/{ticketId}/messages
 */
data class ChatMessage(
    val id: String = "",
    val ticketId: String = "",
    
    // Sender Context
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "user", // "user", "admin", "hatchy", "system"
    
    // Content
    val content: String = "",
    val isInternal: Boolean = false, // If true, only visible to admins
    
    // Attachments (Future Proofing)
    val attachments: List<String> = emptyList(), // List of URLs
    
    // Metadata
    val createdAt: Long? = null,
    val readBy: List<String> = emptyList()
) {
    // Helper to check if message is from the current user
    fun isMine(currentUserId: String): Boolean = senderId == currentUserId
}

