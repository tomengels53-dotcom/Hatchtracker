package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.model.KnowledgeTopic

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.IncubationQueryService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncubationStatusResolver @Inject constructor(
    private val incubationQueryService: IncubationQueryService,
    responseComposer: HatchyResponseComposer
) : QueryResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.INCUBATION_STATUS),
        supportedTopics = setOf(DataTopic.ACTIVE_BATCH_STATUS, KnowledgeTopic.HATCH_TIMING),
        preferredQuestionModes = setOf(QuestionMode.USER_DATA_STATUS),
        priority = ResolverPriority.DATA_QUERY,
        requiresUserData = true,
        requiredEntities = setOf(EntityType.USER_DATA_REF), // Strict requirement
        canReturnConstrainedFallback = false
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val species = interpretation.entities.find { it.type == EntityType.POULTRY_SPECIES }
            ?.value?.let { runCatching { PoultrySpecies.valueOf(it) }.getOrNull() }

        val result = incubationQueryService.resolveIncubationStatusQuery(null, species, context)
        
        if (result.summary.isBlank() || result.confidence < 0.1) {
            return ResolverOutcome.InsufficientEvidence()
        }
        
        val answer = HatchyAnswer(
            text = result.summary,
            type = AnswerType.INCUBATION,
            confidence = calibrateConfidence(
                intentScore = interpretation.confidence,
                entityScore = species?.let { 0.9 } ?: 0.5,
                serviceScore = result.confidence,
                weights = Triple(0.2, 0.4, 0.4)
            ),
            source = result.source,
            debugMetadata = toDebugMetadata("STATUS_QUERY", result.evidence)
        )
        
        return resolveTo(answer)
    }
}
