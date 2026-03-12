package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.ModerationActionLogDto
import com.example.hatchtracker.domain.model.ModerationActionLog
import com.example.hatchtracker.domain.model.ReportReasonCode
import com.example.hatchtracker.domain.model.ReportTargetType
import com.example.hatchtracker.domain.repository.ModerationLogRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModerationActionLogRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ModerationLogRepository {
    private val logsCollection = firestore.collection("moderation_action_logs")

    override suspend fun logAction(action: ModerationActionLog): Result<String> = runCatching {
        val docRef = logsCollection.document()
        val dto = action.toDto().copy(id = docRef.id)
        docRef.set(dto).await()
        docRef.id
    }

    suspend fun getLogsForTarget(targetId: String, targetType: ReportTargetType): List<ModerationActionLog> {
        return logsCollection
            .whereEqualTo("targetId", targetId)
            .whereEqualTo("targetType", targetType.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .map { it.toObject(ModerationActionLogDto::class.java).toDomain() }
    }
}

fun ModerationActionLog.toDto() = ModerationActionLogDto(
    id = id,
    actorUserId = actorUserId,
    actorRole = actorRole,
    targetType = targetType.name,
    targetId = targetId,
    actionType = actionType,
    reasonCode = reasonCode.name,
    moderatorNote = moderatorNote,
    reportId = reportId,
    createdAt = createdAt
)

fun ModerationActionLogDto.toDomain() = ModerationActionLog(
    id = id,
    actorUserId = actorUserId,
    actorRole = actorRole,
    targetType = ReportTargetType.valueOf(targetType),
    targetId = targetId,
    actionType = actionType,
    reasonCode = ReportReasonCode.valueOf(reasonCode),
    moderatorNote = moderatorNote,
    reportId = reportId,
    createdAt = createdAt
)
