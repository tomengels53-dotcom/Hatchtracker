package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.model.HatchyContext

/**
 * The core contract for all Hatchy domain resolvers.
 * Implements a priority-based system and explicit ownership checks.
 */
interface HatchyResolver {
    /**
     * Bounded scoring determining how well this resolver handles the interpretation.
     * Returns a structured result containing the final score and component breakdown.
     */
    fun score(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ScoreResult

    /**
     * Metadata declaring which intents, topics and features this resolver supports.
     */
    val capabilities: ResolverCapabilities

    /**
     * Predicted priority of this resolver family.
     * Tied to capabilities.priority, but kept for easier access.
     */
    val priority: Int get() = capabilities.priority

    /**
     * Full execution path that produces a structured outcome.
     */
    suspend fun resolve(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome
}

/**
 * Tiers for resolver priority.
 */
object ResolverPriority {
    const val USER_DATA = 100
    const val ADVISOR = 90
    const val SIMULATION = 80
    const val DATA_QUERY = 70
    const val GENERAL_KNOWLEDGE = 60
    const val KNOWLEDGE = 50
    const val NAVIGATION = 30
    const val FALLBACK = 0
}

/**
 * Structured result of a resolver scoring operation.
 */
data class ScoreResult(
    val finalScore: Double,
    val components: ResolverScoreComponents,
    val entityRequirementSatisfied: Boolean = true
)

/**
 * Granular components influencing the final resolver score.
 */
data class ResolverScoreComponents(
    val topicMatchScore: Double = 0.0,
    val entityScore: Double = 0.0,
    val questionModeScore: Double = 0.0,
    val userDataScore: Double = 0.0,
    val contextContinuityScore: Double = 0.0,
    val priorityWeight: Double = 0.0
)

/**
 * Weights used to calculate the final weighted score.
 */
data class ScoringWeights(
    val topicMatch: Double = 0.0,
    val entities: Double = 0.0,
    val questionMode: Double = 0.0,
    val userData: Double = 0.0,
    val context: Double = 0.0
)
