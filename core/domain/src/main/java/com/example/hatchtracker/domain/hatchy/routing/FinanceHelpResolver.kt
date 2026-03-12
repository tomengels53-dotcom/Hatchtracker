package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.FinanceQueryService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceHelpResolver @Inject constructor(
    private val financeService: FinanceQueryService,
    responseComposer: HatchyResponseComposer
) : BaseResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.FINANCE_HELP),
        supportedTopics = setOf(WorkflowTopic.LOG_EXPENSE),
        preferredQuestionModes = setOf(QuestionMode.APP_WORKFLOW, QuestionMode.MIXED),
        priority = ResolverPriority.NAVIGATION,
        allowsApproximateMatch = true
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val match = financeService.getHelp(null, context)
            ?: return ResolverOutcome.InsufficientEvidence()
        
        val answer = HatchyAnswer(
            text = match.content,
            type = AnswerType.FINANCE,
            confidence = calibrateConfidence(
                intentScore = interpretation.confidence,
                entityScore = 0.5,
                serviceScore = match.confidence,
                weights = Triple(0.4, 0.4, 0.2)
            ),
            source = match.source,
            suggestedActions = listOf(HatchyAction("Open Finance", "finance")),
            debugMetadata = toDebugMetadata("HELP_MATCH", match.evidence)
        )
        
        return resolveTo(answer)
    }
}
