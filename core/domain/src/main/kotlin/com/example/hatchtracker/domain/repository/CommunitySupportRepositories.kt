package com.example.hatchtracker.domain.repository

import com.example.hatchtracker.domain.model.CommunityReport
import com.example.hatchtracker.domain.model.ModerationActionLog
import com.example.hatchtracker.domain.model.ReportStatus
import com.example.hatchtracker.domain.model.UserBlockRelation
import com.example.hatchtracker.domain.model.UserSafetyState

interface ModerationReportRepository {
    suspend fun getReport(reportId: String): CommunityReport?
    suspend fun updateReportStatus(
        reportId: String,
        status: ReportStatus,
        resolution: String? = null,
        resolvedByUserId: String? = null
    ): Result<Unit>
}

interface UserSafetyStateRepository {
    suspend fun getSafetyState(userId: String): UserSafetyState
    suspend fun updateSafetyState(state: UserSafetyState): Result<Unit>
}

interface ModerationLogRepository {
    suspend fun logAction(action: ModerationActionLog): Result<String>
}

interface UserBlockingRepository {
    suspend fun addBlock(relation: UserBlockRelation): Result<Unit>
    suspend fun removeBlock(blockerId: String, blockedUserId: String): Result<Unit>
    suspend fun isBlocked(blockerId: String, blockedUserId: String): Boolean
    suspend fun getBlockedUserIds(blockerId: String): List<String>
}
