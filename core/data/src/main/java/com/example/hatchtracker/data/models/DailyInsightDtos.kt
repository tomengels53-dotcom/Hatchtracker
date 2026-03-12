package com.example.hatchtracker.data.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.example.hatchtracker.domain.model.*

@IgnoreExtraProperties
data class DailyInsightBatchDto(
    val dateKey: String = "",
    val generatedAt: Long = 0,
    val totalInsightCount: Int = 0,
    val sourceWindowStart: Long = 0,
    val sourceWindowEnd: Long = 0,
    val projectionVersion: Int = 1
)

@IgnoreExtraProperties
data class DailyInsightItemDto(
    val id: String = "",
    val ruleId: String = "",
    val type: String = "HATCH_RATE_DAILY_SUMMARY",
    val title: String = "",
    val body: String = "",
    val hatchyToneVariant: String = "FRIENDLY",
    val confidenceLevel: String = "MEDIUM",
    val rankingScore: Double = 0.0,
    val rankingFactorsSummary: Map<String, Double> = emptyMap(),
    val sourceWindowStart: Long = 0,
    val sourceWindowEnd: Long = 0,
    val comparisonWindowStart: Long? = null,
    val comparisonWindowEnd: Long? = null,
    val metricSummary: Map<String, Any> = emptyMap(),
    val speciesTags: List<String> = emptyList(),
    val breedTags: List<String> = emptyList(),
    val topicTags: List<String> = emptyList(),
    val moderationSafe: Boolean = true,
    val generatedAt: Long = 0,
    val expiresAt: Long? = null,
    val authoredBy: String = "HATCHY"
)

@IgnoreExtraProperties
data class InsightFeedProjectionDto(
    val id: String = "",
    val insightId: String = "",
    val dateKey: String = "",
    val feedSurface: String = "COMMUNITY",
    val cardStyle: String = "INSIGHT",
    val title: String = "",
    val summary: String = "",
    val rankingScore: Double = 0.0,
    val displayPriority: Int = 0,
    val authoredBySnapshot: CommunityAuthorSnapshot? = null,
    val showBadge: Boolean = true,
    val createdAt: Long = 0
)
