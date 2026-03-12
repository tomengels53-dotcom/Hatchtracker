package com.example.hatchtracker.notifications

enum class NotificationType {
    SCHEDULED,
    ENVIRONMENTAL,
    TIMING
}

enum class NotificationSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class NotificationRule(
    val id: String,
    val type: NotificationType,
    val severity: NotificationSeverity,
    val titleTemplate: String,
    val messageTemplate: String,
    val cooldownHours: Int = 24 // Default dedup window
)

data class NotificationEvent(
    val incubationId: Long,
    val ruleId: String,
    val type: NotificationType,
    val severity: NotificationSeverity,
    val title: String,
    val message: String,
    val aiExplanation: String? = null,
    val aiConfidence: String? = null,
    val correctiveAction: String? = null,
    val eventId: String? = null,
    val deeplink: String? = null
)

