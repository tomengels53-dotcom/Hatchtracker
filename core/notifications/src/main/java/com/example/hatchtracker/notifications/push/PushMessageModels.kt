package com.example.hatchtracker.notifications.push

import com.example.hatchtracker.notifications.NotificationSeverity
import com.example.hatchtracker.notifications.NotificationType

data class PushEnvelope(
    val eventId: String,            // required; used for dedupe
    val type: NotificationType,     // e.g. SCHEDULED, ENVIRONMENTAL
    val severityHint: NotificationSeverity?, // optional hint; final severity decided by engine
    val entityId: Long?,            // incubationId/flockId/postId/etc.
    val timestamp: Long,            // server time if provided
    val title: String?,             // optional; engine can override
    val body: String?,              // optional; engine can override
    val deeplink: String?,          // optional; e.g. "hatchy://incubation/123"
    val payload: Map<String, String> = emptyMap()
)
