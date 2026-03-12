package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.DailyInsightBatchDto
import com.example.hatchtracker.data.models.DailyInsightItemDto
import com.example.hatchtracker.data.models.InsightFeedProjectionDto
import com.example.hatchtracker.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class InsightRepository(private val firestore: FirebaseFirestore) {

    fun getFeedProjections(
        surface: String = "COMMUNITY",
        limit: Int = 10
    ): Flow<List<InsightFeedProjection>> = flow {
        val snapshot = firestore.collection("hatchy_insight_feed_projections")
            .whereEqualTo("feedSurface", surface)
            .orderBy("displayPriority", Query.Direction.ASCENDING)
            .orderBy("rankingScore", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val projections = snapshot.toObjects(InsightFeedProjectionDto::class.java).mapNotNull { dto ->
            mapToDomain(dto)
        }
        emit(projections)
    }

    suspend fun getBatchMetadata(dateKey: String): DailyInsightBatch? {
        val snapshot = firestore.collection("daily_hatchbase_insight_batches")
            .document(dateKey)
            .get()
            .await()
        
        return snapshot.toObject(DailyInsightBatchDto::class.java)?.let { dto ->
            DailyInsightBatch(
                dateKey = dto.dateKey,
                generatedAt = dto.generatedAt,
                totalInsightCount = dto.totalInsightCount,
                sourceWindowStart = dto.sourceWindowStart,
                sourceWindowEnd = dto.sourceWindowEnd,
                projectionVersion = dto.projectionVersion
            )
        }
    }

    suspend fun getInsightItem(insightId: String): DailyInsightItem? {
        val snapshot = firestore.collection("daily_hatchbase_insight_items")
            .document(insightId)
            .get()
            .await()

        return snapshot.toObject(DailyInsightItemDto::class.java)?.let { dto ->
            mapToDomain(dto)
        }
    }

    private fun mapToDomain(dto: InsightFeedProjectionDto): InsightFeedProjection? {
        val author = dto.authoredBySnapshot ?: return null
        return InsightFeedProjection(
            id = dto.id,
            insightId = dto.insightId,
            dateKey = dto.dateKey,
            feedSurface = dto.feedSurface,
            cardStyle = dto.cardStyle,
            title = dto.title,
            summary = dto.summary,
            rankingScore = dto.rankingScore,
            displayPriority = dto.displayPriority,
            authoredBySnapshot = author,
            showBadge = dto.showBadge,
            createdAt = dto.createdAt
        )
    }

    private fun mapToDomain(dto: DailyInsightItemDto): DailyInsightItem {
        return DailyInsightItem(
            id = dto.id,
            ruleId = dto.ruleId,
            type = InsightType.valueOf(dto.type),
            title = dto.title,
            body = dto.body,
            hatchyToneVariant = dto.hatchyToneVariant,
            confidenceLevel = ConfidenceLevel.valueOf(dto.confidenceLevel),
            rankingScore = dto.rankingScore,
            rankingFactorsSummary = dto.rankingFactorsSummary,
            windowSummary = InsightWindowSummary(
                sourceWindowStart = dto.sourceWindowStart,
                sourceWindowEnd = dto.sourceWindowEnd,
                comparisonWindowStart = dto.comparisonWindowStart,
                comparisonWindowEnd = dto.comparisonWindowEnd
            ),
            metricSummary = dto.metricSummary,
            speciesTags = dto.speciesTags,
            breedTags = dto.breedTags,
            topicTags = dto.topicTags,
            moderationSafe = dto.moderationSafe,
            generatedAt = dto.generatedAt,
            expiresAt = dto.expiresAt,
            authoredBy = dto.authoredBy
        )
    }
}
