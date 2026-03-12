package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import javax.inject.Singleton

@Singleton
object BreedingProgramConverter {

    fun convert(program: com.example.hatchtracker.domain.breeding.BreedingProgram): com.example.hatchtracker.data.models.BreedingProgram {
        val steps = program.pathway.mapIndexed { index, gen ->
            val instructionBuilder = StringBuilder()
            instructionBuilder.append("Generation F${gen.generationNumber}: Cross ${gen.maleSource} x ${gen.femaleSource}.\n")
            
            if (!gen.selectionGuidance.isNullOrBlank()) {
                instructionBuilder.append("\nSelection Guidance:\n${gen.selectionGuidance}\n")
            }
            if (!gen.retentionCriteria.isNullOrBlank()) {
                instructionBuilder.append("\nRetention Criteria:\n${gen.retentionCriteria}\n")
            }

            val parentSlots = mutableListOf<ParentSlot>()
            
            parentSlots.add(ParentSlot(
                role = ParentRole.SIRE,
                source = if (gen.sireId != null && !gen.sireId.startsWith("-")) "BIRD:${gen.sireId}" 
                         else if (gen.isVirtual && gen.virtualSyncId != null) "VIRTUAL:${gen.virtualSyncId}"
                         else "BREED:${gen.maleSource}",
                displayName = gen.maleSource
            ))
            parentSlots.add(ParentSlot(
                role = ParentRole.DAM,
                source = if (gen.damId != null && !gen.damId.startsWith("-")) "BIRD:${gen.damId}" 
                         else if (gen.isVirtual && gen.virtualSyncId != null) "VIRTUAL:${gen.virtualSyncId}"
                         else "BREED:${gen.femaleSource}",
                displayName = gen.femaleSource
            ))

            val outcomes = gen.expectedTraitGains.toMutableList()
            if (gen.stability != null) {
                outcomes.add("Line Stability: ${gen.stability.glsiScore}%")
                outcomes.add("Fixation Progress: ${(gen.stability.fixationProgress * 100).toInt()}%")
            }

            val riskWarnings = mutableListOf<String>()
            gen.stability?.banner?.let { banner ->
                riskWarnings.add("${banner.title}: ${banner.body}")
            }
            if (gen.stability != null && gen.stability.glsiScore < 50) {
                riskWarnings.add("WARNING: Low stability. Offspring traits will be highly variable.")
            }

            BreedingProgramStep(
                order = index + 1,
                generation = gen.generationNumber,
                title = "Phase ${index + 1}: ${gen.milestoneGoal}",
                instruction = instructionBuilder.toString().trim(),
                requiredParents = parentSlots,
                expectedOutcomes = outcomes,
                riskWarnings = riskWarnings,
                selectionGuidance = gen.selectionGuidance,
                retentionCriteria = gen.retentionCriteria
            )
        }

        return com.example.hatchtracker.data.models.BreedingProgram(
            scenarioId = program.id,
            ownerUserId = "current_user", // Placeholder
            name = "Breeding Plan: ${program.pathway.lastOrNull()?.milestoneGoal ?: "Custom"}",
            steps = steps,
            status = BreedingProgramStatus.ACTIVE
        )
    }
}
