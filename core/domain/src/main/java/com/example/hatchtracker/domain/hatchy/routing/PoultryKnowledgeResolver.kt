package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.KnowledgeTopic

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.PoultryKnowledgeService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves general poultry husbandry, health, and care questions.
 */
@Singleton
class PoultryKnowledgeResolver @Inject constructor(
    private val poultryService: PoultryKnowledgeService,
    responseComposer: HatchyResponseComposer
) : KnowledgeResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.GENERAL_POULTRY, HatchyIntent.POULTRY_HEALTH),
        supportedTopics = setOf(
            KnowledgeTopic.EGG_TRAITS,
            KnowledgeTopic.TEMPERAMENT,
            KnowledgeTopic.HARDINESS,
            KnowledgeTopic.UTILITY_PURPOSE,
            KnowledgeTopic.PHYSICAL_TRAITS,
            KnowledgeTopic.HEALTH_ROBUSTNESS
        ),
        optionalEntities = setOf(EntityType.POULTRY_SPECIES, EntityType.POULTRY_TOPIC),
        preferredQuestionModes = setOf(QuestionMode.REAL_WORLD_GUIDANCE),
        priority = ResolverPriority.KNOWLEDGE,
        allowsApproximateMatch = true,
        canReturnConstrainedFallback = true
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val species = interpretation.entities.find { it.type == EntityType.POULTRY_SPECIES }
            ?.value?.let { runCatching { PoultrySpecies.valueOf(it) }.getOrNull() }
            ?: context.lastSpecies

        val topic = interpretation.entities.find { it.type == EntityType.POULTRY_TOPIC }?.value
            ?: (interpretation.topicResult.primaryTopic as? KnowledgeTopic)?.name

        val match = poultryService.findMatch(interpretation.rawQuery, species, topic, context)

        return if (match != null) {
            val answer = HatchyAnswer(
                text = match.content,
                type = AnswerType.POULTRY_KNOWLEDGE,
                confidence = calibrateConfidence(
                    intentScore = interpretation.confidence,
                    entityScore = if (topic != null) 0.8 else 0.5,
                    serviceScore = match.confidence,
                    weights = Triple(0.4, 0.2, 0.4)
                ),
                source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
                debugMetadata = toDebugMetadata("FOUND", match.evidence)
            )
            resolveTo(answer)
        } else if (capabilities.canReturnConstrainedFallback && topic != null) {
            // Constrained fallback: provide general topic info if species is unknown
            val genericMatch = poultryService.findMatch(interpretation.rawQuery, PoultrySpecies.CHICKEN, topic, context)
            if (genericMatch != null) {
                val answer = HatchyAnswer(
                    text = "In general: ${genericMatch.content}",
                    type = AnswerType.POULTRY_KNOWLEDGE,
                    confidence = AnswerConfidence.MEDIUM,
                    source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
                    debugMetadata = toDebugMetadata("CONSTRAINED_FALLBACK", genericMatch.evidence)
                )
                resolveTo(answer)
            } else {
                ResolverOutcome.InsufficientEvidence()
            }
        } else {
            ResolverOutcome.InsufficientEvidence()
        }
    }
}
