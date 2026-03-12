package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.EquipmentQueryService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentStatusResolver @Inject constructor(
    private val equipmentService: EquipmentQueryService,
    responseComposer: HatchyResponseComposer
) : QueryResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.EQUIPMENT_STATUS),
        supportedTopics = setOf(DataTopic.SENSOR_STATUS, DataTopic.ACTIVE_DEVICES),
        preferredQuestionModes = setOf(QuestionMode.USER_DATA_STATUS),
        priority = ResolverPriority.DATA_QUERY,
        requiresUserData = true,
        canReturnConstrainedFallback = false
    )

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val catEntity = interpretation.entities.find { it.type == EntityType.EQUIPMENT_CAT }
        val category = catEntity?.value?.let { runCatching { EquipmentCategory.valueOf(it) }.getOrNull() }

        val result = equipmentService.resolveEquipmentStatusQuery(null, category, context)
        
        if (result.summary.isBlank() || result.confidence < 0.1) {
            return ResolverOutcome.InsufficientEvidence()
        }
        
        val answer = HatchyAnswer(
            text = result.summary,
            type = AnswerType.EQUIPMENT,
            confidence = calibrateConfidence(
                intentScore = interpretation.confidence,
                entityScore = catEntity?.confidence ?: 0.5,
                serviceScore = result.confidence,
                weights = Triple(0.2, 0.4, 0.4)
            ),
            source = result.source,
            debugMetadata = toDebugMetadata("EQUIPMENT_QUERY", result.evidence)
        )
        
        return resolveTo(answer)
    }
}
