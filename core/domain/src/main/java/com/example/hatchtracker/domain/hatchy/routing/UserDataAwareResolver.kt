package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.IncubationQueryService
import com.example.hatchtracker.domain.hatchy.routing.services.NurseryQueryService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves general questions about the user's data (flocks, active batches).
 */
@Singleton
class UserDataAwareResolver @Inject constructor(
    private val nurseryService: NurseryQueryService,
    private val incubationService: IncubationQueryService,
    responseComposer: HatchyResponseComposer
) : QueryResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.USER_DATA_QUERY),
        preferredQuestionModes = setOf(QuestionMode.USER_DATA_STATUS),
        priority = ResolverPriority.USER_DATA,
        requiresUserData = true,
        allowsApproximateMatch = true,
        canReturnConstrainedFallback = false
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val q = interpretation.rawQuery.lowercase()
        val intent = interpretation.intent

        val result = when {
            q.contains("incubating") || q.contains("batch") || intent == HatchyIntent.INCUBATION_STATUS -> {
                incubationService.resolveIncubationStatusQuery(null, null, context)
            }
            else -> {
                nurseryService.resolveNurseryStatusQuery(null, null, context)
            }
        }

        val type = when {
            q.contains("incubating") || q.contains("batch") || intent == HatchyIntent.INCUBATION_STATUS -> AnswerType.INCUBATION
            else -> AnswerType.NURSERY
        }

        val answer = HatchyAnswer(
            text = result.summary,
            type = type,
            confidence = calibrateConfidence(
                intentScore = interpretation.confidence,
                entityScore = 0.5, // General query
                serviceScore = result.confidence,
                weights = Triple(0.2, 0.4, 0.4)
            ),
            source = result.source,
            debugMetadata = toDebugMetadata("GENERAL_DATA_QUERY", result.evidence)
        )
        
        return resolveTo(answer)
    }
}
