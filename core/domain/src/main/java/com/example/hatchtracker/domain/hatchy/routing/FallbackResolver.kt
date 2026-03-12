package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.EvidenceMetadata
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premium fallback resolver that acknowledges limits and suggests alternatives.
 */
@Singleton
class FallbackResolver @Inject constructor(
    responseComposer: HatchyResponseComposer
) : BaseResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = HatchyIntent.values().toSet(),
        priority = ResolverPriority.FALLBACK,
        allowsApproximateMatch = true
    )

    override fun score(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ScoreResult = ScoreResult(
        finalScore = 0.11, // Always slightly above the rejection threshold (0.1)
        components = ResolverScoreComponents(),
        entityRequirementSatisfied = true
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val evidence = EvidenceMetadata(
            matchScore = 0.0,
            matchedTopic = "FALLBACK",
            dataSourceId = "none"
        )
        
        val answer = HatchyAnswer(
            text = "I don’t have a specific answer for your question right now, but I can help you with breeding strategies, incubation tracking, nursery management, and financial summaries for your flock.",
            type = AnswerType.FALLBACK,
            confidence = AnswerConfidence.VERY_LOW,
            source = AnswerSource.FALLBACK,
            debugMetadata = toDebugMetadata("SYSTEM_FALLBACK", evidence)
        )
        return resolveTo(answer)
    }
}
