package com.example.hatchtracker.model

enum class AuditActionType {
    CREATE, UPDATE, DELETE, PROMOTE, DEPRECATE, SECURITY_EVENT,
    SUBSCRIPTION_OVERRIDE, SEED_SCRIPT
}

data class AdminAuditLog(
    val id: String = "", // Auto-generated
    val adminUserId: String = "",
    val adminEmail: String = "", // For easier human reading
    val actionType: AuditActionType = AuditActionType.UPDATE,
    val targetCollection: String = "", // e.g., "breedStandards", "users"
    val targetDocumentId: String = "",
    
    // Snapshots for Diffing (stored as Maps for Firestore flexibility)
    val beforeSnapshot: Map<String, Any?>? = null,
    val afterSnapshot: Map<String, Any?>? = null,
    
    // Trace Fields
    val appVersion: String = "",
    val buildType: String = "",
    val deviceModel: String = "",
    
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null, // Admin-provided justification
    val clientVersion: String? = null, // Deprecated, use appVersion
    val ipHash: String? = null // Optional anonymized IP
)

