package com.example.hatchtracker.domain.breeding

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strategy scoring result with breakdown.
 */
data class StrategyScore(
    val totalScore: Int,
    val breakdown: List<ScoreExplanation>
)

data class ScoreExplanation(
    val label: String,
    val impact: Int,
    val reason: String
)

/**
 * Scorer for breeding plans.
 */
@Singleton
class PlanScorer @Inject constructor() {

    fun scorePlan(
        goalMatches: Double, // 0.0 to 1.0
        inbreedingRisk: Double, // 0.0 to 1.0 (COI based)
        bottleneckRisk: Double, // 0.0 to 1.0 (Ne based)
        uncertainty: Double, // 0.0 to 1.0
        diversityBonus: Double
    ): StrategyScore {
        val breakdown = mutableListOf<ScoreExplanation>()
        var total = 0

        // Goal Match (Max 100)
        val goalImpact = (goalMatches * 100).toInt()
        total += goalImpact
        breakdown.add(ScoreExplanation("Goal Alignment", goalImpact, "Based on target trait probabilities."))

        // Inbreeding Penalty (Max -25)
        val inbreedingPenalty = (inbreedingRisk * -25).toInt()
        total += inbreedingPenalty
        breakdown.add(ScoreExplanation("Inbreeding Penalty", inbreedingPenalty, "Based on Coefficient of Inbreeding (COI)."))

        // Bottleneck Penalty (Max -15)
        val bottleneckPenalty = (bottleneckRisk * -15).toInt()
        total += bottleneckPenalty
        breakdown.add(ScoreExplanation("Bottleneck Penalty", bottleneckPenalty, "Based on Effective Population Size (Ne)."))

        // Uncertainty Penalty (Max -20)
        val uncertaintyPenalty = (uncertainty * -20).toInt()
        total += uncertaintyPenalty
        breakdown.add(ScoreExplanation("Uncertainty Penalty", uncertaintyPenalty, "Based on unknown parent genotypes."))

        // Diversity Bonus (Max 15)
        val divBonus = (diversityBonus * 15).toInt()
        total += divBonus
        breakdown.add(ScoreExplanation("Diversity Bonus", divBonus, "Based on unique sire utilization."))

        return StrategyScore(total.coerceAtLeast(0), breakdown)
    }
}
