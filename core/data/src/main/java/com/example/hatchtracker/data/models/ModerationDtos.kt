package com.example.hatchtracker.data.models

import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for common report reasons.
 */
data class CommunityReportDto(
    val id: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val reportedUserId: String = "",
    val reporterUserId: String = "",
    val reasonCode: String = "",
    val details: String = "",
    val status: String = "OPEN",
    val resolution: String? = null,
    val targetPreviewSnapshot: String? = null,
    val createdAt: Long = 0,
    val resolvedAt: Long? = null,
    val resolvedByUserId: String? = null
)

/**
 * Firestore DTO for the moderation queue read-model.
 */
data class ModerationQueueEntryDto(
    val reportId: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val topReason: String = "",
    val reportCount: Int = 0,
    val latestReportAt: Long = 0,
    @get:PropertyName("queuePriority") @set:PropertyName("queuePriority") var queuePriority: Int = 0,
    var severity: Int = 0,
    val targetPreview: String? = null,
    val status: String = "OPEN"
)

/**
 * Firestore DTO for user safety and restriction status.
 */
data class UserSafetyStateDto(
    val userId: String = "",
    var strikeCount: Int = 0,
    @get:PropertyName("isSuspended") @set:PropertyName("isSuspended") var isSuspended: Boolean = false,
    val suspensionUntil: Long? = null,
    val postingRestrictedUntil: Long? = null,
    val commentingRestrictedUntil: Long? = null,
    val marketplaceRestrictedUntil: Long? = null,
    val lastModeratedAt: Long? = null,
    val moderatorNotes: String? = null
)

/**
 * Firestore DTO for strike records.
 */
data class ModerationStrikeDto(
    val id: String = "",
    val userId: String = "",
    val actionId: String = "",
    val reasonCode: String = "",
    val weight: Int = 1,
    val issuedAt: Long = 0,
    val expiresAt: Long? = null
)

/**
 * Firestore DTO for action logs.
 */
data class ModerationActionLogDto(
    val id: String = "",
    val actorUserId: String = "",
    val actorRole: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val actionType: String = "",
    val reasonCode: String = "",
    val moderatorNote: String? = null,
    val reportId: String? = null,
    val createdAt: Long = 0
)

/**
 * Firestore DTO for blocking relations.
 */
data class UserBlockRelationDto(
    val blockerUserId: String = "",
    val blockedUserId: String = "",
    val createdAt: Long = 0
)
