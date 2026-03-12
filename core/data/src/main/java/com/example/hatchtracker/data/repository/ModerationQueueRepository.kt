package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.ModerationQueueEntryDto
import com.example.hatchtracker.domain.model.ModerationQueueEntry
import com.example.hatchtracker.domain.model.ReportReasonCode
import com.example.hatchtracker.domain.model.ReportStatus
import com.example.hatchtracker.domain.model.ReportTargetType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModerationQueueRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val queueCollection = firestore.collection("moderation_queue_projections")

    /**
     * Fetch prioritized queue entries.
     */
    suspend fun getQueue(limit: Long = 50): List<ModerationQueueEntry> {
        return queueCollection
            .orderBy("queuePriority", Query.Direction.DESCENDING)
            .orderBy("latestReportAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .map { it.toObject(ModerationQueueEntryDto::class.java).toDomain() }
    }

    suspend fun getEntry(reportId: String): ModerationQueueEntry? {
        val doc = queueCollection.document(reportId).get().await()
        return doc.toObject(ModerationQueueEntryDto::class.java)?.toDomain()
    }
}

fun ModerationQueueEntryDto.toDomain() = ModerationQueueEntry(
    reportId = reportId,
    targetType = ReportTargetType.valueOf(targetType),
    targetId = targetId,
    topReason = ReportReasonCode.valueOf(topReason),
    reportCount = reportCount,
    latestReportAt = latestReportAt,
    queuePriority = queuePriority,
    severity = severity,
    targetPreview = targetPreview,
    status = ReportStatus.valueOf(status)
)
