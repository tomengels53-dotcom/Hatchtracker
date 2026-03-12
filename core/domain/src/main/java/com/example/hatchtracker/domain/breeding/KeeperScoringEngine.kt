package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.ExecutionStage
import com.example.hatchtracker.data.models.GoalSpec
import com.example.hatchtracker.data.models.StrategyConfig
import com.example.hatchtracker.data.models.StrategyMode
import com.example.hatchtracker.data.models.TelemetrySnapshot
import com.example.hatchtracker.model.Bird

enum class ScoreBucket {
    RECOMMENDED, CONSIDER, OFF_PLAN
}

data class ScoreBreakdown(
    val goalAlignment0to100: Int,
    val geneticValue0to100: Int,
    val performance0to100: Int,
    val stageBonus0to100: Int,
    val penalties: List<String> = emptyList()
)

data class KeeperScore(
    val birdId: String,
    val total0to100: Int,
    val bucket: ScoreBucket,
    val breakdown: ScoreBreakdown,
    val reason: String
)

class KeeperScoringEngine {

    fun scoreCandidates(
        strategyConfig: StrategyConfig,
        stage: ExecutionStage,
        candidates: List<Bird>,
        telemetry: TelemetrySnapshot,
        mode: StrategyMode
    ): List<KeeperScore> {
        return candidates.map { bird ->
            val breakdown = calculateBreakdown(bird, strategyConfig.goalSpecs, telemetry, mode)
            val total = (
                breakdown.goalAlignment0to100 * 0.4 +
                    breakdown.geneticValue0to100 * 0.3 +
                    breakdown.performance0to100 * 0.2 +
                    breakdown.stageBonus0to100 * 0.1
                ).toInt()
            val bucket = when {
                total >= 80 -> ScoreBucket.RECOMMENDED
                total >= 50 -> ScoreBucket.CONSIDER
                else -> ScoreBucket.OFF_PLAN
            }
            KeeperScore(
                birdId = bird.cloudId,
                total0to100 = total,
                bucket = bucket,
                breakdown = breakdown,
                reason = generateReason(total, breakdown, mode)
            )
        }.sortedByDescending { it.total0to100 }
    }

    private fun calculateBreakdown(
        bird: Bird,
        goals: List<GoalSpec>,
        telemetry: TelemetrySnapshot,
        mode: StrategyMode
    ): ScoreBreakdown {
        val alignment = calculateAlignment(bird, goals)
        val geneticValue = if (mode == StrategyMode.STRICT_LINE_BREEDING) 85 else 75
        val performance = if (telemetry.sampleSize >= 5) 75 else 65
        val stageBonus = 10
        val penalties = mutableListOf<String>()
        if (mode == StrategyMode.COMMERCIAL_PRODUCTION && alignment < 50) penalties.add("Low production alignment")
        if (mode == StrategyMode.STRICT_LINE_BREEDING && alignment < 30) penalties.add("Low line-breeding alignment")
        return ScoreBreakdown(
            goalAlignment0to100 = alignment,
            geneticValue0to100 = geneticValue,
            performance0to100 = performance,
            stageBonus0to100 = stageBonus,
            penalties = penalties
        )
    }

    private fun calculateAlignment(bird: Bird, goals: List<GoalSpec>): Int {
        if (goals.isEmpty()) return 100
        val overrides = bird.geneticProfile.traitOverrides
        val matches = goals.count { goal ->
            overrides.any { it.traitId == goal.traitKey && it.optionId == goal.targetValue }
        }
        return (matches.toDouble() / goals.size * 100).toInt()
    }

    private fun generateReason(total: Int, breakdown: ScoreBreakdown, mode: StrategyMode): String {
        return when {
            total >= 80 -> "Strong candidate for next generation."
            breakdown.penalties.isNotEmpty() -> breakdown.penalties.joinToString(", ")
            total < 40 -> "Does not meet minimum requirements for $mode."
            else -> "Acceptable candidate for supplemental breeding."
        }
    }
}

