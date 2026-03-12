package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.FinanceQueryService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceSummaryResolver @Inject constructor(
    private val financeService: FinanceQueryService,
    responseComposer: HatchyResponseComposer
) : QueryResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.FINANCE_SUMMARY),
        supportedTopics = setOf(DataTopic.TOTAL_SPEND, DataTopic.CATEGORY_BREAKDOWN, DataTopic.MONTHLY_TREND, DataTopic.FLOCK_COST),
        preferredQuestionModes = setOf(QuestionMode.USER_DATA_STATUS),
        priority = ResolverPriority.DATA_QUERY,
        requiresUserData = true,
        canReturnConstrainedFallback = false
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val topic = interpretation.topicResult.primaryTopic as? DataTopic

        val periodEntity = interpretation.entities.find { it.type == EntityType.FINANCE_PERIOD }
        val period = periodEntity?.value?.let { runCatching { FinancePeriod.valueOf(it) }.getOrNull() }

        val result = financeService.resolveFinanceSummaryQuery(null, period, context)
        
        if (result.summary.isBlank() || result.confidence < 0.1) {
            return ResolverOutcome.InsufficientEvidence()
        }
        
        val answer = HatchyAnswer(
            text = result.summary,
            type = AnswerType.FINANCE,
            confidence = calibrateConfidence(
                intentScore = interpretation.confidence,
                entityScore = periodEntity?.confidence ?: 0.5,
                serviceScore = result.confidence,
                weights = Triple(0.2, 0.4, 0.4)
            ),
            source = result.source,
            debugMetadata = toDebugMetadata("FINANCE_QUERY", result.evidence)
        )
        
        return resolveTo(answer)
    }
}
