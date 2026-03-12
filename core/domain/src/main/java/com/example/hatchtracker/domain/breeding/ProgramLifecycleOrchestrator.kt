package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramLifecycleOrchestrator @Inject constructor(
    private val planRepository: BreedingProgramRepository,
    private val incubationRepository: IncubationRepository
) {
    private companion object {
        const val MIXED_BREED_LABEL = "Mixed Breed"
    }


    suspend fun onIncubationCreated(
        incubationId: Long,
        planId: String,
        generationIndex: Int,
        breederPoolBirdIds: List<String>
    ): Result<Unit> {
        val planResult = planRepository.getPlan(planId)
        val plan = planResult.getOrNull() ?: return Result.failure(Exception("Plan not found"))
        
        val incubationResult = incubationRepository.getIncubationById(incubationId)
        val incubation = incubationResult ?: return Result.failure(Exception("Incubation not found"))

        // 1. Update Incubation with Action Plan metadata
        val updatedIncubation = incubation.copy(
            actionPlanId = planId,
            generationIndex = generationIndex,
            breedLabelOverride = MIXED_BREED_LABEL,
            breederPoolBirdIds = breederPoolBirdIds
        )
        incubationRepository.update(updatedIncubation, "Linked to Action Plan: ${plan.name} (Gen F$generationIndex)")

        // 2. Add INCUBATION link to Plan
        val updatedAssets = plan.linkedAssets + BreedingProgramAssetLink(
            type = AssetType.INCUBATION,
            refId = incubation.cloudId,
            role = LinkRole.INCUBATION,
            generationIndex = generationIndex
        )

        // 3. Update activeGenerationIndex if this is a new generation
        val newActiveGen = if (generationIndex > plan.activeGenerationIndex) {
            generationIndex
        } else {
            plan.activeGenerationIndex
        }

        // 4. Append audit entry
        val auditLog = plan.auditLog + BreedingProgramAuditEntry(
            message = "Incubation created (${incubation.cloudId}) for Gen F$generationIndex. Breed set to $MIXED_BREED_LABEL."
        )

        val updatedPlan = plan.copy(
            linkedAssets = updatedAssets,
            activeGenerationIndex = newActiveGen,
            auditLog = auditLog,
            updatedAt = System.currentTimeMillis()
        )

        return planRepository.updatePlan(updatedPlan)
    }
}
