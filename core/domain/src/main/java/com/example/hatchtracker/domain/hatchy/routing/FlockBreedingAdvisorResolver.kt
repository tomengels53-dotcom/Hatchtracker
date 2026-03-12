package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.KnowledgeTopic

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.FlockBreedingAdvisorService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlockBreedingAdvisorResolver @Inject constructor(
    private val breedingService: FlockBreedingAdvisorService,
    responseComposer: HatchyResponseComposer
) : RecommendationResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.USER_FLOCK_RECOMMENDATION),
        supportedTopics = setOf(KnowledgeTopic.GOAL_BASED_PAIRING),
        optionalEntities = setOf(EntityType.BREEDING_GOAL, EntityType.POULTRY_SPECIES),
        preferredQuestionModes = setOf(QuestionMode.REAL_WORLD_GUIDANCE, QuestionMode.MIXED),
        priority = ResolverPriority.ADVISOR,
        requiresUserData = true,
        canReturnConstrainedFallback = false
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val goalEntity = interpretation.entities.find { it.type == EntityType.BREEDING_GOAL }
        val goal = goalEntity?.value?.let { runCatching { BreedingGoal.valueOf(it) }.getOrNull() }
        val species = interpretation.entities.find { it.type == EntityType.POULTRY_SPECIES }
            ?.value?.let { runCatching { PoultrySpecies.valueOf(it) }.getOrNull() }

        val result = breedingService.getRecommendation(goal, species, context)
        
        if (result.reasoning.isBlank() || result.confidence < 0.1) {
            return ResolverOutcome.InsufficientEvidence()
        }
        
        val answer = HatchyAnswer(
            text = result.reasoning,
            type = AnswerType.RECOMMENDATION,
            confidence = calibrateConfidence(
                intentScore = interpretation.confidence,
                entityScore = goalEntity?.confidence ?: 0.5,
                serviceScore = result.confidence,
                weights = Triple(0.2, 0.3, 0.5)
            ),
            source = result.source,
            debugMetadata = toDebugMetadata("ADVISOR_QUERY", result.evidence)
        )
        
        return resolveTo(answer)
    }
}
