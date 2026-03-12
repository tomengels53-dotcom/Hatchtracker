package com.example.hatchtracker.domain.hatchy.routing

import javax.inject.Inject
import javax.inject.Singleton

enum class QuestionMode {
    REAL_WORLD_GUIDANCE,
    APP_WORKFLOW,
    USER_DATA_STATUS,
    MIXED,
    UNKNOWN // For queries that don't match any anchors
}

data class QuestionModeResult(
    val primaryMode: QuestionMode,
    val secondaryMode: QuestionMode?,
    val modeConfidence: Double,
    val appAnchorScore: Double,
    val realWorldAnchorScore: Double,
    val userDataAnchorScore: Double
)

@Singleton
class QuestionModeClassifier @Inject constructor() {

    private val appAnchors = listOf("app", "here", "in hatchy", "where do i log", "where can i", "module", "feature", "screen", "menu", "track", "save", "record")
    private val realWorldAnchors = listOf("set up", "setup", "install", "calibrate", "clean", "prepare", "feed", "symptoms", "humidity", "temperature", "should i", "why are", "how to")
    private val userDataAnchors = listOf("my", "currently", "how many", "active", "closest to hatch", "this week", "this month")

    companion object {
        // Tunable threshold for considering a mode "strong"
        const val STRONG_THRESHOLD = 1.0
        // Margin allowed between top two scores to classify as MIXED
        const val MIXED_MARGIN = 0.5
    }

    fun classify(query: String): QuestionModeResult {
        val q = query.lowercase()

        var appScore = 0.0
        var realWorldScore = 0.0
        var userDataScore = 0.0

        appAnchors.forEach { if (q.contains(it)) appScore += 1.0 }
        realWorldAnchors.forEach { if (q.contains(it)) realWorldScore += 1.0 }
        userDataAnchors.forEach { if (q.contains(it)) userDataScore += 1.0 }

        val scores = listOf(
            Pair(QuestionMode.USER_DATA_STATUS, userDataScore),
            Pair(QuestionMode.REAL_WORLD_GUIDANCE, realWorldScore),
            Pair(QuestionMode.APP_WORKFLOW, appScore)
        ).sortedByDescending { it.second }

        val top = scores[0]
        val second = scores[1]

        val primaryMode: QuestionMode
        val secondaryMode: QuestionMode?
        val confidence: Double

        if (top.second >= STRONG_THRESHOLD && second.second >= STRONG_THRESHOLD && (top.second - second.second) <= MIXED_MARGIN) {
            primaryMode = QuestionMode.MIXED
            // Provide the top competing mode as secondary information
            secondaryMode = top.first
            confidence = 0.8
        } else if (top.second >= STRONG_THRESHOLD) {
            primaryMode = top.first
            secondaryMode = if (second.second > 0.0) second.first else null
            confidence = 1.0
        } else {
            primaryMode = QuestionMode.UNKNOWN
            secondaryMode = null
            // Reduced confidence for UNKNOWN so we continue routing but normally rank lower
            confidence = 0.5
        }

        return QuestionModeResult(
            primaryMode = primaryMode,
            secondaryMode = secondaryMode,
            modeConfidence = confidence,
            appAnchorScore = appScore,
            realWorldAnchorScore = realWorldScore,
            userDataAnchorScore = userDataScore
        )
    }
}
