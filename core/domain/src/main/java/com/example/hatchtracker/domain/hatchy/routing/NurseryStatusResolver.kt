package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.DataTopic

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.NurseryQueryService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NurseryStatusResolver @Inject constructor(
    private val nurseryService: NurseryQueryService,
    responseComposer: HatchyResponseComposer
) : QueryResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.NURSERY_STATUS),
        supportedTopics = setOf(DataTopic.LOSSES_SUMMARY, DataTopic.ACTIVE_CHICK_COUNT, DataTopic.AGE_GROUP_SUMMARY),
        preferredQuestionModes = setOf(QuestionMode.USER_DATA_STATUS),
        priority = ResolverPriority.DATA_QUERY,
        requiresUserData = true,
        canReturnConstrainedFallback = false
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val species = interpretation.entities.find { it.type == EntityType.POULTRY_SPECIES }
            ?.value?.let { runCatching { PoultrySpecies.valueOf(it) }.getOrNull() }

        val result = nurseryService.resolveNurseryStatusQuery(null, species, context)
        
        if (result.summary.isBlank() || result.confidence < 0.1) {
            return ResolverOutcome.InsufficientEvidence()
        }
        
        val answer = HatchyAnswer(
            text = result.summary,
            type = AnswerType.NURSERY,
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
