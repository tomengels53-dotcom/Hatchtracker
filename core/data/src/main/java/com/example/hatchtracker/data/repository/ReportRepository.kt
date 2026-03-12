package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.CommunityReportDto
import com.example.hatchtracker.domain.model.CommunityReport
import com.example.hatchtracker.domain.model.ReportReasonCode
import com.example.hatchtracker.domain.model.ReportStatus
import com.example.hatchtracker.domain.model.ReportTargetType
import com.example.hatchtracker.domain.repository.ModerationReportRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ModerationReportRepository {
    private val reportsCollection = firestore.collection("community_reports")

    suspend fun createReport(report: CommunityReport): Result<String> = runCatching {
        val docRef = if (report.id.isEmpty()) {
            reportsCollection.document()
        } else {
            reportsCollection.document(report.id)
        }
        val dto = report.toDto().copy(id = docRef.id)
        docRef.set(dto).await()
        docRef.id
    }

    override suspend fun getReport(reportId: String): CommunityReport? {
        val doc = reportsCollection.document(reportId).get().await()
        return doc.toObject(CommunityReportDto::class.java)?.toDomain()
    }

    override suspend fun updateReportStatus(
        reportId: String, 
        status: ReportStatus, 
        resolution: String?,
        resolvedByUserId: String?
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "status" to status.name
        )
        resolution?.let { updates["resolution"] = it }
        resolvedByUserId?.let { 
            updates["resolvedByUserId"] = it
            updates["resolvedAt"] = System.currentTimeMillis()
        }
        reportsCollection.document(reportId).update(updates).await()
    }
}

fun CommunityReport.toDto() = CommunityReportDto(
    id = id,
    targetType = targetType.name,
    targetId = targetId,
    reportedUserId = reportedUserId,
    reporterUserId = reporterUserId,
    reasonCode = reasonCode.name,
    details = details,
    status = status.name,
    resolution = resolution,
    targetPreviewSnapshot = targetPreviewSnapshot,
    createdAt = createdAt,
    resolvedAt = resolvedAt,
    resolvedByUserId = resolvedByUserId
)

fun CommunityReportDto.toDomain() = CommunityReport(
    id = id,
    targetType = ReportTargetType.valueOf(targetType),
    targetId = targetId,
    reportedUserId = reportedUserId,
    reporterUserId = reporterUserId,
    reasonCode = ReportReasonCode.valueOf(reasonCode),
    details = details,
    status = ReportStatus.valueOf(status),
    resolution = resolution,
    targetPreviewSnapshot = targetPreviewSnapshot,
    createdAt = createdAt,
    resolvedAt = resolvedAt,
    resolvedByUserId = resolvedByUserId
)
