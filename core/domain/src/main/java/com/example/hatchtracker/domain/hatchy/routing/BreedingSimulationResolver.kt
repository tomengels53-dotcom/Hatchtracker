package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.CrossbreedingSimulationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BreedingSimulationResolver @Inject constructor(
    private val breedingService: CrossbreedingSimulationService,
    responseComposer: HatchyResponseComposer
) : RecommendationResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.CROSSBREED_OUTCOME),
        supportedTopics = setOf(KnowledgeTopic.CROSSBREED_RECOMMENDATION),
        requiredEntities = setOf(),
        optionalEntities = setOf(EntityType.BREED),
        preferredQuestionModes = setOf(QuestionMode.REAL_WORLD_GUIDANCE, QuestionMode.MIXED),
        priority = ResolverPriority.SIMULATION,
        canReturnConstrainedFallback = false
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val breeds = interpretation.entities.filter { it.type == EntityType.BREED }
        val breedValues = breeds.map { it.value }
        
        val pair = if (breedValues.size >= 2) {
            Pair(breedValues[0], breedValues[1])
        } else if (breedValues.size == 1 && context.recentBreedsMentioned.isNotEmpty()) {
            Pair(breedValues[0], context.recentBreedsMentioned.first())
        } else null

        if (pair == null) return ResolverOutcome.InsufficientEvidence()

        val result = breedingService.simulate(pair.first, pair.second, context)
        
        if (result.reasoning.isBlank() || result.confidence < 0.1) {
            return ResolverOutcome.InsufficientEvidence()
        }
        
        val answer = HatchyAnswer(
            text = result.reasoning,
            type = AnswerType.CROSSBREEDING,
            confidence = calibrateConfidence(
                intentScore = interpretation.confidence,
                entityScore = if (breeds.isNotEmpty()) breeds.map { it.confidence }.average() else 0.5,
                serviceScore = result.confidence,
                weights = Triple(0.2, 0.3, 0.5)
            ),
            source = result.source,
            relatedEntities = breeds,
            debugMetadata = toDebugMetadata("SIMULATION", result.evidence)
        )
        
        return resolveTo(answer)
    }
}
