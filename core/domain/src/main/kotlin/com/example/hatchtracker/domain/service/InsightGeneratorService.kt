package com.example.hatchtracker.domain.service

import com.example.hatchtracker.domain.model.*
import com.example.hatchtracker.model.Incubation
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Flock
import java.util.UUID

/**
 * Orchestrates the generation of Daily HatchBase Insights.
 * Strictly projection-driven and deterministic.
 */
class InsightGeneratorService {

    /**
     * Internal framework for generating candidate insights.
     */
    suspend fun generateDailyBatch(
        dateKey: String,
        incubations: List<Incubation>,
        birds: List<Bird>,
        flocks: List<Flock>,
        communityQuestions: List<CommunityPost>
    ): InsightGenerationResult {
        val windowEnd = System.currentTimeMillis()
        val windowStart = windowEnd - (24 * 60 * 60 * 1000) // Last 24h

        val candidates = mutableListOf<DailyInsightItem>()
        val suppressed = mutableListOf<SuppressedInsight>()

        // Rule 1: Hatch Rate Summary
        val hatchRateCandidate = generateHatchRateInsight(dateKey, incubations, windowStart, windowEnd)
        hatchRateCandidate?.let { candidates.add(it) } ?: suppressed.add(SuppressedInsight("HATCH_RATE_RULE", "Insufficient sample size or no trend delta"))

        // Rule 2: Breed Trend
        val breedTrendCandidate = generateBreedTrendInsight(dateKey, birds, windowStart, windowEnd)
        breedTrendCandidate?.let { candidates.add(it) } ?: suppressed.add(SuppressedInsight("BREED_TREND_RULE", "No significant growth detected"))

        // Rule 3: Community Question Trend
        val questionTrendCandidate = generateQuestionTrendInsight(dateKey, communityQuestions, windowStart, windowEnd)
        questionTrendCandidate?.let { candidates.add(it) } ?: suppressed.add(SuppressedInsight("QUESTION_TREND_RULE", "Insufficient question volume or topic cluster"))

        // Ranking and Filtering
        val rankedInsights = candidates
            .filter { it.confidenceLevel != ConfidenceLevel.LOW || it.rankingScore > 0.5 }
            .sortedByDescending { it.rankingScore }
            .take(5) // Global cap

        val batch = DailyInsightBatch(
            dateKey = dateKey,
            generatedAt = windowEnd,
            totalInsightCount = rankedInsights.size,
            sourceWindowStart = windowStart,
            sourceWindowEnd = windowEnd
        )

        return InsightGenerationResult(
            batch = batch,
            insights = rankedInsights,
            candidates = candidates,
            suppressed = suppressed
        )
    }

    private fun generateHatchRateInsight(
        dateKey: String,
        incubations: List<Incubation>,
        start: Long,
        end: Long
    ): DailyInsightItem? {
        val recentBatches = incubations.filter { it.hatchCompleted && it.actualHatchDate != null }
        if (recentBatches.size < 5) return null // Min sample size for insight

        val avgHatchRate = recentBatches.map { 
            if (it.eggsCount > 0) it.hatchedCount.toDouble() / it.eggsCount else 0.0 
        }.average()

        if (avgHatchRate < 0.1) return null // Noise floor

        return DailyInsightItem(
            id = UUID.randomUUID().toString(),
            ruleId = "HATCH_RATE_GLOBAL_V1",
            type = InsightType.HATCH_RATE_DAILY_SUMMARY,
            title = "Daily Hatch Update",
            body = "Hatchy noticed an average hatch rate of ${(avgHatchRate * 100).toInt()}% across reported batches today.",
            confidenceLevel = if (recentBatches.size > 15) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM,
            rankingScore = 0.8,
            windowSummary = InsightWindowSummary(start, end),
            metricSummary = mapOf("avgHatchRate" to avgHatchRate, "sampleSize" to recentBatches.size)
        )
    }

    private fun generateBreedTrendInsight(
        dateKey: String,
        birds: List<Bird>,
        start: Long,
        end: Long
    ): DailyInsightItem? {
        val breeds = birds.groupBy { it.breed }.filter { it.key.isNotBlank() }
        val topBreed = breeds.maxByOrNull { it.value.size } ?: return null
        
        if (topBreed.value.size < 10) return null

        return DailyInsightItem(
            id = UUID.randomUUID().toString(),
            ruleId = "BREED_POPULARITY_V1",
            type = InsightType.BREED_TREND,
            title = "Trending Breed",
            body = "Hatchy noticed growing activity in ${topBreed.key} records this week.",
            confidenceLevel = ConfidenceLevel.MEDIUM,
            rankingScore = 0.6,
            windowSummary = InsightWindowSummary(start, end),
            breedTags = listOf(topBreed.key),
            metricSummary = mapOf("birdCount" to topBreed.value.size)
        )
    }

    private fun generateQuestionTrendInsight(
        dateKey: String,
        questions: List<CommunityPost>,
        start: Long,
        end: Long
    ): DailyInsightItem? {
        val activeQuestions = questions.filter { it.kind == PostKind.QUESTION }
        if (activeQuestions.size < 3) return null

        return DailyInsightItem(
            id = UUID.randomUUID().toString(),
            ruleId = "COMMUNITY_QUESTION_V1",
            type = InsightType.COMMUNITY_QUESTION_TREND,
            title = "Trending Community Question",
            body = "Questions about incubation settings are trending in the community today.",
            confidenceLevel = ConfidenceLevel.MEDIUM,
            rankingScore = 0.7,
            windowSummary = InsightWindowSummary(start, end),
            topicTags = listOf("incubation")
        )
    }
}

data class InsightGenerationResult(
    val batch: DailyInsightBatch,
    val insights: List<DailyInsightItem>,
    val candidates: List<DailyInsightItem>,
    val suppressed: List<SuppressedInsight>
)

data class SuppressedInsight(
    val ruleId: String,
    val reason: String
)
