package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.EvidenceMetadata

/**
 * Abstract base for all resolvers to reduce boilerplate and enforce consistency.
 */
abstract class BaseResolver(
    protected val responseComposer: HatchyResponseComposer
) : HatchyResolver {

    abstract override val capabilities: ResolverCapabilities

    /**
     * Default weights for this resolver family.
     */
    protected open val scoringWeights: ScoringWeights = ScoringWeights(
        topicMatch = 0.4,
        entities = 0.2,
        questionMode = 0.2,
        userData = 0.1,
        context = 0.1
    )

    override fun score(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ScoreResult {
        // 1. Intent match (Hard Filter)
        if (interpretation.intent !in capabilities.supportedIntents) {
            return ScoreResult(0.0, ResolverScoreComponents(), false)
        }

        // 2. Component Scoring
        val topicMatchScore = if (interpretation.topicResult.primaryTopic in capabilities.supportedTopics) 1.0 else 0.0
        
        val inputEntities = interpretation.entities.map { it.type }.toSet()
        val hasRequired = capabilities.requiredEntities.isEmpty() || 
                         capabilities.requiredEntities.all { it in inputEntities }
        
        val entityScore = if (hasRequired) 1.0 else 0.0
        
        val questionModeScore = if (interpretation.questionMode.primaryMode in capabilities.preferredQuestionModes) 1.0 else 0.0
        
        val userDataScore = if (capabilities.requiresUserData) {
            if (interpretation.entities.any { it.type == EntityType.USER_DATA_REF } || context.hasUserDataContext) 1.0 else 0.0
        } else 0.0

        val contextContinuityScore = if (capabilities.supportsFollowUpContext && 
                                        (context.lastTopic == (interpretation.topicResult.primaryTopic as? Enum<*>)?.name || 
                                         context.lastSpecies != null)) 0.5 else 0.0

        val components = ResolverScoreComponents(
            topicMatchScore = topicMatchScore,
            entityScore = entityScore,
            questionModeScore = questionModeScore,
            userDataScore = userDataScore,
            contextContinuityScore = contextContinuityScore,
            priorityWeight = priority.toDouble() / 1000.0 // Priority is tiny tie-breaker
        )

        // 3. Weighting & Penalty Rule
        var finalScore = (components.topicMatchScore * scoringWeights.topicMatch) +
                        (components.entityScore * scoringWeights.entities) +
                        (components.questionModeScore * scoringWeights.questionMode) +
                        (components.userDataScore * scoringWeights.userData) +
                        (components.contextContinuityScore * scoringWeights.context) +
                        components.priorityWeight

        // Entity Penalty Rule: Zero score if required entities are missing 
        // unless approximation or fallback is allowed.
        if (!hasRequired && !capabilities.allowsApproximateMatch && !capabilities.canReturnConstrainedFallback) {
            finalScore = 0.0
        }

        return ScoreResult(
            finalScore = finalScore.coerceIn(0.0, 1.0),
            components = components,
            entityRequirementSatisfied = hasRequired
        )
    }

    override suspend fun resolve(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        return resolveQuery(interpretation, context)
    }

    protected abstract suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome

    /**
     * Helper to wrap an answer in a Resolved outcome.
     */
    protected fun resolveTo(answer: HatchyAnswer): ResolverOutcome {
        return ResolverOutcome.Resolved(answer)
    }

    /**
     * Maps EvidenceMetadata to standardized Hatchy debug metadata.
     */
    protected fun toDebugMetadata(
        outcome: String,
        evidence: EvidenceMetadata
    ): Map<String, Any> {
        return mutableMapOf<String, Any>(
            "resolver" to this::class.java.simpleName,
            "outcome" to outcome
        ).apply {
            evidence.matchedTopic?.let { put("matchedTopic", it) }
            evidence.matchedSubtype?.let { put("matchedSubtype", it) }
            evidence.matchedSpecies?.let { put("matchedSpecies", it.name) }
            put("matchScore", evidence.matchScore)
            evidence.recordCount?.let { put("recordCount", it) }
            evidence.candidateCount?.let { put("candidateCount", it) }
            evidence.dataSourceId?.let { put("dataSourceId", it) }
            evidence.knowledgeKey?.let { put("knowledgeKey", it) }
            evidence.sourcePath?.let { put("sourcePath", it) }
            putAll(evidence.customMetadata)
        }
    }

    /**
     * Standardized confidence calibration with family-specific weighting.
     */
    protected fun calibrateConfidence(
        intentScore: Double,
        entityScore: Double,
        serviceScore: Double,
        weights: Triple<Double, Double, Double> = Triple(0.33, 0.33, 0.34)
    ): AnswerConfidence {
        var score = (intentScore * weights.first) + (entityScore * weights.second) + (serviceScore * weights.third)
        
        // Safety Rule: LOW confidence from entity/intent scores must never be hidden by HIGH service score.
        // If the query understanding is shaky, the final result must be at most MEDIUM.
        if (intentScore < 0.5 || entityScore < 0.5) {
            score = score.coerceAtMost(0.6)
        }
        
        return when {
            score >= 0.85 -> AnswerConfidence.HIGH
            score >= 0.6 -> AnswerConfidence.MEDIUM
            score >= 0.3 -> AnswerConfidence.LOW
            else -> AnswerConfidence.VERY_LOW
        }
    }
}

/**
 * Base for knowledge-backed resolvers (Incubation, Health, etc.)
 */
abstract class KnowledgeResolver(
    responseComposer: HatchyResponseComposer
) : BaseResolver(responseComposer) {
    override val scoringWeights: ScoringWeights = ScoringWeights(
        topicMatch = 0.5,
        questionMode = 0.3,
        entities = 0.2
    )
}

/**
 * Base for query/data-backed resolvers (Nursery, Finance, etc.)
 */
abstract class QueryResolver(
    responseComposer: HatchyResponseComposer
) : BaseResolver(responseComposer) {
    override val scoringWeights: ScoringWeights = ScoringWeights(
        userData = 0.6,
        topicMatch = 0.4
    )
}

/**
 * Base for recommendation/simulation resolvers (Advisor, Engine)
 */
abstract class RecommendationResolver(
    responseComposer: HatchyResponseComposer
) : BaseResolver(responseComposer) {
    override val scoringWeights: ScoringWeights = ScoringWeights(
        context = 0.4,
        topicMatch = 0.3,
        entities = 0.3
    )
}
