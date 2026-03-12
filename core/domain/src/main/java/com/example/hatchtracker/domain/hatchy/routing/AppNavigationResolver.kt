package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.EvidenceMetadata
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNavigationResolver @Inject constructor(
    responseComposer: HatchyResponseComposer
) : BaseResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.APP_NAVIGATION),
        preferredQuestionModes = setOf(QuestionMode.APP_WORKFLOW, QuestionMode.MIXED),
        priority = ResolverPriority.NAVIGATION,
        allowsApproximateMatch = true,
        canReturnConstrainedFallback = false
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val module = interpretation.module ?: context.currentModule
        
        val text = when (module.uppercase()) {
            "FLOCK" -> "To manage your birds, go to the Flock module. You can add new birds, track health, and view heritage there."
            "INCUBATION" -> "The Incubation module is for tracking active hatches. Tap the '+' button to start a new batch."
            "NURSERY" -> "Use the Nursery to track your growing chicks. It helps you monitor temperatures and mortality rates."
            "FINANCE" -> "The Finance module helps you track expenses and income. You can log feed costs and bird sales here."
            "EQUIPMENT" -> "Manage your smart incubators and sensors in the Equipment module."
            else -> "I can help you navigate to any part of the app. Just ask about your flocks, hatches, or finances!"
        }
        
        val evidence = EvidenceMetadata(
            matchScore = interpretation.confidence,
            matchedTopic = "navigation",
            dataSourceId = "app_navigation_map"
        )

        val answer = HatchyAnswer(
            text = text,
            type = AnswerType.NAVIGATION,
            confidence = calibrateConfidence(
                intentScore = interpretation.confidence,
                entityScore = 1.0,
                serviceScore = 0.8,
                weights = Triple(0.4, 0.4, 0.2)
            ),
            source = AnswerSource.APP_KNOWLEDGE_BASE,
            suggestedActions = listOf(HatchyAction("Open $module", module.lowercase())),
            debugMetadata = toDebugMetadata("NAVIGATION", evidence)
        )
        
        return resolveTo(answer)
    }
}
