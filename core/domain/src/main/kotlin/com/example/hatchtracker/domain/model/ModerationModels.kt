package com.example.hatchtracker.domain.model

/**
 * Standardized reason codes for reporting content or users.
 */
enum class ReportReasonCode {
    SPAM,
    HARASSMENT,
    MISINFORMATION,
    ILLEGAL_SALE,
    ANIMAL_WELFARE_CONCERN,
    COUNTERFEIT_OR_SCAM,
    INAPPROPRIATE_CONTENT,
    DUPLICATE_LISTING,
    OTHER
}

enum class ReportTargetType {
    COMMUNITY_POST,
    COMMUNITY_COMMENT,
    MARKETPLACE_LISTING,
    USER
}

enum class ReportStatus {
    OPEN,
    IN_REVIEW,
    ESCALATED,
    RESOLVED_ACTION_TAKEN,
    RESOLVED_NO_ACTION,
    DISMISSED
}

/**
 * Domain model for a community report.
 */
data class CommunityReport(
    val id: String = "",
    val targetType: ReportTargetType,
    val targetId: String,
    val reportedUserId: String, // Identity of the user being reported
    val reporterUserId: String, // Identity of the user filing the report (Non-public)
    val reasonCode: ReportReasonCode,
    val details: String = "",
    val status: ReportStatus = ReportStatus.OPEN,
    val resolution: String? = null,
    val targetPreviewSnapshot: String? = null, // Truncated preview or JSON snapshot for context
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolvedByUserId: String? = null
)

/**
 * Read-model projection for the moderation queue.
 */
data class ModerationQueueEntry(
    val reportId: String,
    val targetType: ReportTargetType,
    val targetId: String,
    val topReason: ReportReasonCode,
    val reportCount: Int,
    val latestReportAt: Long,
    val queuePriority: Int = 0, // Server-trusted priority score
    val severity: Int = 0,      // Server-trusted severity level
    val targetPreview: String? = null,
    val status: ReportStatus = ReportStatus.OPEN
)

/**
 * User safety state including temporary restrictions.
 */
data class UserSafetyState(
    val userId: String,
    val strikeCount: Int = 0,
    val isSuspended: Boolean = false,
    val suspensionUntil: Long? = null,
    val postingRestrictedUntil: Long? = null,
    val commentingRestrictedUntil: Long? = null,
    val marketplaceRestrictedUntil: Long? = null,
    val lastModeratedAt: Long? = null,
    val moderatorNotes: String? = null
)

/**
 * Record of an individual moderation strike.
 */
data class ModerationStrike(
    val id: String = "",
    val userId: String,
    val actionId: String, // Reference to the ModerationActionLog
    val reasonCode: ReportReasonCode,
    val weight: Int = 1, // Future-safe: some strikes might count more than others
    val issuedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null // Future-safe: support for expiring strikes
)

/**
 * Immutable audit log for moderator actions.
 */
data class ModerationActionLog(
    val id: String = "",
    val actorUserId: String,
    val actorRole: String, // e.g. "CommunityAdmin"
    val targetType: ReportTargetType,
    val targetId: String,
    val actionType: String, // e.g. "STRIKE_ISSUED", "CONTENT_REMOVED"
    val reasonCode: ReportReasonCode,
    val moderatorNote: String? = null,
    val reportId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
