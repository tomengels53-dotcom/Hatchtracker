package com.example.hatchtracker.domain.service

import com.example.hatchtracker.domain.model.*
import com.example.hatchtracker.domain.repository.ModerationLogRepository
import com.example.hatchtracker.domain.repository.ModerationReportRepository
import com.example.hatchtracker.domain.repository.UserSafetyStateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModerationService @Inject constructor(
    private val reportRepository: ModerationReportRepository,
    private val safetyRepository: UserSafetyStateRepository,
    private val logRepository: ModerationLogRepository
) {
    /**
     * Resolve a report with a specific action.
     */
    suspend fun resolveReport(
        reportId: String,
        moderatorId: String,
        moderatorRole: String,
        status: ReportStatus,
        resolutionNote: String,
        isActionTaken: Boolean = false
    ): Result<Unit> = runCatching {
        val report = reportRepository.getReport(reportId) ?: throw Exception("Report not found")
        
        // 1. Update Report Status
        reportRepository.updateReportStatus(
            reportId = reportId,
            status = status,
            resolution = resolutionNote,
            resolvedByUserId = moderatorId
        )

        // 2. Log Action
        if (isActionTaken) {
            val log = ModerationActionLog(
                actorUserId = moderatorId,
                actorRole = moderatorRole,
                targetType = report.targetType,
                targetId = report.targetId,
                actionType = "REPORT_RESOLVED_WITH_ACTION",
                reasonCode = report.reasonCode,
                moderatorNote = resolutionNote,
                reportId = reportId
            )
            logRepository.logAction(log)
        }
    }

    /**
     * Issue a strike to a user.
     */
    suspend fun issueStrike(
        userId: String,
        moderatorId: String,
        moderatorRole: String,
        reason: ReportReasonCode,
        note: String
    ): Result<Unit> = runCatching {
        val safetyState = safetyRepository.getSafetyState(userId)
        
        // 1. Create Log Entry
        val log = ModerationActionLog(
            actorUserId = moderatorId,
            actorRole = moderatorRole,
            targetType = ReportTargetType.USER,
            targetId = userId,
            actionType = "STRIKE_ISSUED",
            reasonCode = reason,
            moderatorNote = note
        )
        logRepository.logAction(log).getOrThrow()

        // 2. Update Safety State (Progressive escalation could be added here)
        val updatedState = safetyState.copy(
            strikeCount = safetyState.strikeCount + 1,
            lastModeratedAt = System.currentTimeMillis()
        )
        safetyRepository.updateSafetyState(updatedState).getOrThrow()
    }

    /**
     * Apply a temporary restriction to a user.
     */
    suspend fun applyRestriction(
        userId: String,
        moderatorId: String,
        moderatorRole: String,
        restrictionType: String, // e.g. "POSTING", "MARKETPLACE"
        durationMs: Long,
        reason: ReportReasonCode,
        note: String
    ): Result<Unit> = runCatching {
        val safetyState = safetyRepository.getSafetyState(userId)
        val now = System.currentTimeMillis()
        val expiry = now + durationMs
        
        val updatedState = when (restrictionType) {
            "POSTING" -> safetyState.copy(postingRestrictedUntil = expiry)
            "COMMENTING" -> safetyState.copy(commentingRestrictedUntil = expiry)
            "MARKETPLACE" -> safetyState.copy(marketplaceRestrictedUntil = expiry)
            "SUSPENSION" -> safetyState.copy(isSuspended = true, suspensionUntil = expiry)
            else -> throw Exception("Unknown restriction type")
        }

        safetyRepository.updateSafetyState(updatedState.copy(lastModeratedAt = now)).getOrThrow()
        
        logRepository.logAction(ModerationActionLog(
            actorUserId = moderatorId,
            actorRole = moderatorRole,
            targetType = ReportTargetType.USER,
            targetId = userId,
            actionType = "RESTRICTION_APPLIED: $restrictionType",
            reasonCode = reason,
            moderatorNote = "Duration: ${durationMs / 1000 / 60} mins. Note: $note"
        )).getOrThrow()
    }
}
