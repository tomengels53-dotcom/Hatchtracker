package com.example.hatchtracker.notifications

data class NotificationPayload(
    val title: String,
    val body: String,
    val severity: NotificationSeverity,
    val incubationId: Long,
    val species: String,
    val deepLinkUri: String? = null,
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // Default 24h expiry
    val aiExplanation: String? = null,
    val aiConfidence: String? = null,
    val correctiveAction: String? = null
)

