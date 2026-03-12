package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.NurseryKnowledgeService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NurseryGuidanceResolver @Inject constructor(
    private val nurseryService: NurseryKnowledgeService,
    responseComposer: HatchyResponseComposer
) : KnowledgeResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.NURSERY_GUIDANCE),
        supportedTopics = setOf(KnowledgeTopic.BROODER_TEMPERATURE, KnowledgeTopic.READY_TO_MOVE, KnowledgeTopic.EARLY_CHICK_CARE),
        optionalEntities = setOf(EntityType.POULTRY_SPECIES, EntityType.NURSERY_GUIDANCE_TOPIC),
        preferredQuestionModes = setOf(QuestionMode.REAL_WORLD_GUIDANCE),
        priority = ResolverPriority.KNOWLEDGE,
        allowsApproximateMatch = true,
        canReturnConstrainedFallback = true
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val speciesEntity = interpretation.entities.find { it.type == EntityType.POULTRY_SPECIES }
        val species = speciesEntity?.value?.let { runCatching { PoultrySpecies.valueOf(it) }.getOrNull() }
            ?: context.lastSpecies

        val topic = interpretation.entities.find { it.type == EntityType.NURSERY_GUIDANCE_TOPIC }?.value
            ?: when (interpretation.topicResult.primaryTopic) {
                KnowledgeTopic.BROODER_TEMPERATURE -> "TEMPERATURE"
                KnowledgeTopic.READY_TO_MOVE -> "COOP_TRANSITION"
                KnowledgeTopic.EARLY_CHICK_CARE -> "EARLY_CARE"
                else -> null
            }

        val match = nurseryService.findMatch(interpretation.rawQuery, species, topic, context)

        return if (match != null) {
            val answer = HatchyAnswer(
                text = match.content,
                type = AnswerType.NURSERY,
                confidence = calibrateConfidence(
                    intentScore = interpretation.confidence,
                    entityScore = speciesEntity?.confidence ?: 0.5,
                    serviceScore = match.confidence,
                    weights = Triple(0.6, 0.2, 0.2)
                ),
                source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
                debugMetadata = toDebugMetadata("KNOWLEDGE_MATCH", match.evidence)
            )
            resolveTo(answer)
        } else {
            ResolverOutcome.InsufficientEvidence()
        }
    }
}
