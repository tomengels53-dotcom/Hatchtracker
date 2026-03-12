package com.example.hatchtracker.domain.model

/**
 * Represents the daily generation run control for insights.
 */
data class DailyInsightBatch(
    val dateKey: String, // ISO date string (e.g. 2026-03-06)
    val generatedAt: Long = System.currentTimeMillis(),
    val totalInsightCount: Int = 0,
    val sourceWindowStart: Long = 0,
    val sourceWindowEnd: Long = 0,
    val projectionVersion: Int = 1
)

/**
 * Represents a normalized, single insight record.
 */
data class DailyInsightItem(
    val id: String = "",
    val ruleId: String, // e.g. HATCH_RATE_DELTA_V1
    val type: InsightType,
    val title: String,
    val body: String,
    val hatchyToneVariant: String = "FRIENDLY",
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.MEDIUM,
    val rankingScore: Double = 0.0,
    val rankingFactorsSummary: Map<String, Double> = emptyMap(),
    val windowSummary: InsightWindowSummary,
    val metricSummary: Map<String, Any> = emptyMap(),
    val speciesTags: List<String> = emptyList(),
    val breedTags: List<String> = emptyList(),
    val topicTags: List<String> = emptyList(),
    val moderationSafe: Boolean = true,
    val generatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val authoredBy: String = "HATCHY"
)

enum class InsightType {
    HATCH_RATE_DAILY_SUMMARY,
    BREED_TREND,
    SPECIES_ACTIVITY_TREND,
    COMMUNITY_QUESTION_TREND,
    COLLABORATIVE_PROJECT_TREND,
    BREEDING_PROGRAM_TREND,
    MARKETPLACE_ACTIVITY_TREND,
    COMMUNITY_EXPERT_RESPONSE_TREND
}

enum class ConfidenceLevel {
    LOW, MEDIUM, HIGH, VERY_HIGH
}

data class InsightWindowSummary(
    val sourceWindowStart: Long,
    val sourceWindowEnd: Long,
    val comparisonWindowStart: Long? = null,
    val comparisonWindowEnd: Long? = null
)

/**
 * UI-optimized read model for feed rendering.
 */
data class InsightFeedProjection(
    val id: String = "",
    val insightId: String,
    val dateKey: String,
    val feedSurface: String = "COMMUNITY", // HOME, COMMUNITY
    val cardStyle: String = "INSIGHT",
    val title: String,
    val summary: String,
    val rankingScore: Double = 0.0,
    val displayPriority: Int = 0,
    val authoredBySnapshot: CommunityAuthorSnapshot,
    val showBadge: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
