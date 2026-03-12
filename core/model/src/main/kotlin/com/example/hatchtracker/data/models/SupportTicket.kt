package com.example.hatchtracker.data.models

import java.util.Date

/**
 * High-level area of the app for categorization.
 */
data class SupportModule(
    val id: String = "",
    val name: String = ""
)

/**
 * Specific feature inside a module.
 */
data class SupportFeature(
    val id: String = "",
    val moduleId: String = "",
    val name: String = ""
)

/**
 * Represents a user-submitted support ticket.
 * Aligned with 'tickets' root collection schema.
 */
data class SupportTicket(
    val ticketId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    
    // Categorization
    val category: String = "",
    val type: String = "",
    val categoryDetail: TicketCategory = TicketCategory(),
    
    val status: TicketStatus = TicketStatus.SUBMITTED,
    val priority: TicketPriority = TicketPriority.MEDIUM,
    val subscriptionTierAtCreation: SubscriptionTier = SubscriptionTier.FREE,
    
    // Content
    val subject: String = "",
    val description: String = "",
    
    // Metadata
    val deviceInfo: Map<String, String> = emptyMap(),
    val appVersion: String = "",

    // Diagnostics
    val diagnostics: SupportDiagnostics? = null,
    
    // Timestamps
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    
    // AI / Future
    val sentimentScore: Double = 0.0,
    val tags: List<String> = emptyList(),
    
    // Hatchy Triage Result
    val hatchyTriage: Map<String, Any> = emptyMap(),

    // Structured Requests
    val changeRequest: ChangeRequest? = null,

    // Approval Metadata
    val approvedAt: Date? = null,
    val approvedBy: String? = null
)

data class SupportDiagnostics(
    val appVersionName: String = "",
    val appVersionCode: Long = 0L,
    val deviceModel: String = "",
    val androidVersion: String = "",
    val userId: String = "",
    val subscriptionTier: String = "",
    val logLines: List<String> = emptyList(),
    val workerRuns: List<WorkerRunStatus> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

data class WorkerRunStatus(
    val workerName: String = "",
    val lastSuccessAt: Long? = null,
    val lastFailureAt: Long? = null,
    val lastFailureMessage: String? = null
)

/**
 * Metadata for specific requested changes (e.g. Country/Currency).
 */
data class ChangeRequest(
    val type: String = "", // e.g. "COUNTRY_CHANGE"
    val oldValue: String = "",
    val newValue: String = "",
    val reason: String = "",
    val userAcknowledgement: Boolean = false,
    val adminNotes: String? = null,
    val resolvedAt: Long? = null
)

data class TicketCategory(
    val moduleId: String = "",
    val moduleName: String = "",
    val featureId: String = "",
    val featureName: String = ""
)

enum class TicketStatus {
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    RESOLVED,
    REJECTED,
    // Legacy statuses (kept for backward compatibility)
    OPEN,
    IN_PROGRESS,
    WAITING_FOR_USER,
    CLOSED
}

enum class TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

