package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility to import legacy BreedingScenarios into the new BreedingProgram system
 * by mapping them to BreedingActionPlans with status DRAFT.
 */
@Singleton
class LegacyScenarioImporter @Inject constructor() {

    fun import(
        scenario: com.example.hatchtracker.data.models.BreedingScenario
    ): com.example.hatchtracker.data.models.BreedingProgram {
        val steps = scenario.generations.map { gen ->
            val instructionBuilder = StringBuilder()
            instructionBuilder.append("[Imported from legacy scenario]\n")
            if (gen.description.isNotBlank()) {
                instructionBuilder.append("${gen.description}\n")
            }
            if (gen.pairings.isNotEmpty()) {
                instructionBuilder.append("\nPairings:\n")
                gen.pairings.forEach { pairing ->
                    instructionBuilder.append("- ${pairing.maleSource} x ${pairing.femaleSource}: ${pairing.rationale}\n")
                }
            }

            val parentSlots = mutableListOf<ParentSlot>()
            gen.pairings.forEach { pairing ->
                // Basic mapping: use the male/female source as display names
                parentSlots.add(ParentSlot(
                    role = ParentRole.SIRE,
                    source = "BREED:${pairing.maleSource}",
                    displayName = pairing.maleSource
                ))
                parentSlots.add(ParentSlot(
                    role = ParentRole.DAM,
                    source = "BREED:${pairing.femaleSource}",
                    displayName = pairing.femaleSource
                ))
            }

            BreedingProgramStep(
                order = gen.generationIndex,
                generation = gen.generationIndex,
                title = "Generation F${gen.generationIndex}",
                instruction = instructionBuilder.toString().trim(),
                requiredParents = parentSlots,
                expectedOutcomes = gen.predictedOutcomes,
                riskWarnings = emptyList(),
                selectionGuidance = gen.aiGuidanceSnapshot,
                retentionCriteria = null
            )
        }

        return com.example.hatchtracker.data.models.BreedingProgram(
            id = java.util.UUID.randomUUID().toString(),
            scenarioId = scenario.id,
            ownerUserId = scenario.ownerUserId,
            name = "[LEGACY] ${scenario.name}",
            createdAt = scenario.timestamp,
            updatedAt = System.currentTimeMillis(),
            status = BreedingProgramStatus.DRAFT,
            steps = steps,
            planSpecies = com.example.hatchtracker.model.Species.valueOf(scenario.species.uppercase()),
            finalGeneration = scenario.generations.maxByOrNull { it.generationIndex }?.generationIndex ?: 1
        )
    }
}

