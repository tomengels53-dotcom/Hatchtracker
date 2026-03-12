package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.BreedingGuidanceKnowledgeService
import com.example.hatchtracker.model.KnowledgeTopic
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BreedingGuidanceResolver @Inject constructor(
    private val breedingService: BreedingGuidanceKnowledgeService,
    responseComposer: HatchyResponseComposer
) : KnowledgeResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.BREEDING_GUIDANCE),
        supportedTopics = setOf(
            KnowledgeTopic.BREEDING_STRATEGY,
            KnowledgeTopic.TRAIT_INHERITANCE,
            KnowledgeTopic.GENERATION_VARIATION
        ),
        optionalEntities = setOf(EntityType.POULTRY_SPECIES, EntityType.BREEDING_TOPIC),
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

        val topic = interpretation.entities.find { it.type == EntityType.BREEDING_TOPIC }?.value
            ?: when (interpretation.topicResult.primaryTopic) {
                KnowledgeTopic.BREEDING_STRATEGY -> "SELECTION"
                KnowledgeTopic.TRAIT_INHERITANCE -> "GENETICS"
                KnowledgeTopic.GENERATION_VARIATION -> "GENERATION_VARIATION"
                else -> null
            }

        val result = if (topic != null) {
            breedingService.findMatch(interpretation.rawQuery, species, topic, context)
        } else null

        return if (result != null) {
            val answer = HatchyAnswer(
                text = result.content,
                type = AnswerType.GUIDANCE,
                confidence = calibrateConfidence(
                    intentScore = interpretation.confidence,
                    entityScore = if (species != null) 0.8 else 0.5,
                    serviceScore = result.confidence,
                    weights = Triple(0.3, 0.3, 0.4)
                ),
                source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
                debugMetadata = toDebugMetadata("FOUND", result.evidence)
            )
            resolveTo(answer)
        } else if (capabilities.canReturnConstrainedFallback && topic != null) {
            // Constrained fallback: provide general breeding guidance if species is unknown
            val genericResult = breedingService.findMatch(interpretation.rawQuery, PoultrySpecies.CHICKEN, topic, context)
            if (genericResult != null) {
                val answer = HatchyAnswer(
                    text = "Generally: ${genericResult.content}",
                    type = AnswerType.GUIDANCE,
                    confidence = AnswerConfidence.MEDIUM,
                    source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
                    debugMetadata = toDebugMetadata("CONSTRAINED_FALLBACK", genericResult.evidence)
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
