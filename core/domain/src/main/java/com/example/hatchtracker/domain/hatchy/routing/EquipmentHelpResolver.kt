package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.EquipmentQueryService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentHelpResolver @Inject constructor(
    private val equipmentService: EquipmentQueryService,
    responseComposer: HatchyResponseComposer
) : BaseResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.EQUIPMENT_HELP),
        supportedTopics = setOf(
            KnowledgeTopic.CALIBRATE_DEVICE,
            KnowledgeTopic.CLEAN_DEVICE,
            KnowledgeTopic.MAINTENANCE_DUE
        ),
        optionalEntities = setOf(EntityType.EQUIPMENT_HELP_TOPIC, EntityType.EQUIPMENT_CAT),
        preferredQuestionModes = setOf(QuestionMode.REAL_WORLD_GUIDANCE, QuestionMode.MIXED),
        priority = ResolverPriority.NAVIGATION,
        allowsApproximateMatch = true
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val topic = (interpretation.topicResult.primaryTopic as? KnowledgeTopic)?.name

        val catEntity = interpretation.entities.find { it.type == EntityType.EQUIPMENT_CAT }
        val category = catEntity?.value?.let { runCatching { EquipmentCategory.valueOf(it) }.getOrNull() }
        
        val match = equipmentService.getHelp(null, category, context)
            ?: return ResolverOutcome.InsufficientEvidence()
        
        val answer = HatchyAnswer(
            text = match.content,
            type = AnswerType.EQUIPMENT,
            confidence = calibrateConfidence(
                intentScore = interpretation.confidence,
                entityScore = catEntity?.confidence ?: 0.5,
                serviceScore = match.confidence,
                weights = Triple(0.4, 0.4, 0.2)
            ),
            source = match.source,
            suggestedActions = listOf(HatchyAction("View Equipment", "equipment")),
            debugMetadata = toDebugMetadata("HELP_MATCH", match.evidence)
        )
        
        return resolveTo(answer)
    }
}
