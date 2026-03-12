package com.example.hatchtracker.domain.hatchy.model

import java.util.UUID

enum class SenderType {
    USER,
    HATCHY,
    SYSTEM
}

enum class FeedbackType {
    NONE,
    THUMBS_UP,
    THUMBS_DOWN
}

data class HatchyChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val scenarioId: String,
    val text: String,
    val sender: SenderType,
    val timestamp: Long = System.currentTimeMillis(),
    val feedback: FeedbackType = FeedbackType.NONE,
    val debugMetadata: Map<String, Any>? = null
)
