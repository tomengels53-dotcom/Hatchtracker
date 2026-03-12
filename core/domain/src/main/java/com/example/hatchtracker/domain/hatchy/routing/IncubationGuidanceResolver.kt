package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.IncubationKnowledgeService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncubationGuidanceResolver @Inject constructor(
    private val incubationService: IncubationKnowledgeService,
    responseComposer: HatchyResponseComposer
) : KnowledgeResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.INCUBATION_GUIDANCE),
        supportedTopics = setOf(
            KnowledgeTopic.TEMPERATURE,
            KnowledgeTopic.HUMIDITY,
            KnowledgeTopic.TURNING,
            KnowledgeTopic.LOCKDOWN,
            KnowledgeTopic.HATCH_TIMING,
            KnowledgeTopic.INCUBATION_PERIOD,
            KnowledgeTopic.SETUP_DEVICE
        ),
        optionalEntities = setOf(EntityType.POULTRY_SPECIES, EntityType.INCUBATION_TOPIC),
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

        val topic = when (interpretation.topicResult.primaryTopic as? KnowledgeTopic) {
            KnowledgeTopic.INCUBATION_PERIOD -> "DURATION"
            KnowledgeTopic.HATCH_TIMING -> "DURATION"
            KnowledgeTopic.TEMPERATURE -> "TEMPERATURE"
            KnowledgeTopic.HUMIDITY -> "HUMIDITY"
            KnowledgeTopic.TURNING -> "TURNING"
            KnowledgeTopic.LOCKDOWN -> "LOCKDOWN"
            KnowledgeTopic.SETUP_DEVICE -> null
            else -> null
        }
            ?: interpretation.entities.find { it.type == EntityType.INCUBATION_TOPIC }?.value

        val match = incubationService.findMatch(interpretation.rawQuery, species, topic, context)

        return if (match != null) {
            val answer = HatchyAnswer(
                text = match.content,
                type = AnswerType.INCUBATION,
                confidence = calibrateConfidence(
                    intentScore = interpretation.confidence,
                    entityScore = if (topic != null) 1.0 else 0.5,
                    serviceScore = match.confidence,
                    weights = Triple(0.4, 0.2, 0.4)
                ),
                source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
                debugMetadata = toDebugMetadata("FOUND", match.evidence)
            )
            resolveTo(answer)
        } else {
            ResolverOutcome.InsufficientEvidence()
        }
    }
}
