package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.model.Species

object ScenarioToProgramConverter {

    fun convert(
        scenario: com.example.hatchtracker.data.models.BreedingScenario
    ): com.example.hatchtracker.data.models.BreedingProgram {
        val steps = scenario.generations
            .sortedBy { it.generationIndex }
            .mapIndexed { index, generation ->
                val instructionBuilder = StringBuilder()
                val parentSlots = mutableListOf<ParentSlot>()
                val outcomes = mutableListOf<String>()
                
                if (index == 0) {
                    instructionBuilder.append("### F1: Initial Foundation Cross\n")
                    instructionBuilder.append("Establish your foundation lines using the following pairings:\n\n")
                } else {
                    instructionBuilder.append("### F${generation.generationIndex}: Genetic Refinement\n")
                    instructionBuilder.append("Select the best offspring from the previous generation to stabilize desired traits:\n\n")
                }

                generation.pairings.forEachIndexed { pIndex, pairing ->
                    val maleDisplay = formatSource(pairing.maleSource)
                    val femaleDisplay = formatSource(pairing.femaleSource)
                    
                    instructionBuilder.append("  â€¢ **Pairing ${pIndex + 1}**: $maleDisplay Ã— $femaleDisplay\n")
                    if (pairing.rationale.isNotBlank()) {
                         instructionBuilder.append("    _Rationale: ${pairing.rationale}_\n")
                    }
                    
                    parentSlots.add(ParentSlot(
                        role = ParentRole.SIRE,
                        source = pairing.maleSource,
                        displayName = maleDisplay
                    ))
                    parentSlots.add(ParentSlot(
                        role = ParentRole.DAM,
                        source = pairing.femaleSource,
                        displayName = femaleDisplay
                    ))
                }
                
                outcomes.addAll(generation.predictedOutcomes)
                
                if (outcomes.isNotEmpty()) {
                    instructionBuilder.append("\n**Primary Objectives**: ${outcomes.joinToString(", ")}\n")
                }

                // High Severity Risks
                val highRisks = scenario.riskWarnings.filter { 
                    it.severity.equals("High", ignoreCase = true) || it.severity.equals("Lethal", ignoreCase = true)
                }
                
                if (highRisks.isNotEmpty() && index == 0) {
                    instructionBuilder.append("\n> [!WARNING]\n")
                    highRisks.forEach { risk ->
                        instructionBuilder.append("> **${risk.severity}**: ${risk.description}\n")
                    }
                }

                BreedingProgramStep(
                    order = index + 1,
                    generation = generation.generationIndex,
                    title = "Generation F${generation.generationIndex}",
                    instruction = instructionBuilder.toString().trim(),
                    requiredParents = parentSlots.distinctBy { it.source },
                    expectedOutcomes = outcomes,
                    riskWarnings = highRisks.map { "${it.severity}: ${it.description}" },
                    selectionGuidance = if (index > 0) "Prioritize offspring that most closely match the target traits from F1." else null
                )
            }

        val speciesEnum = try { 
            com.example.hatchtracker.model.Species.valueOf(scenario.species.uppercase()) 
        } catch (e: Exception) { 
            com.example.hatchtracker.model.Species.CHICKEN 
        }

        return com.example.hatchtracker.data.models.BreedingProgram(
            id = "plan_${scenario.id}_${System.currentTimeMillis()}",
            scenarioId = scenario.id,
            ownerUserId = scenario.ownerUserId,
            name = scenario.name,
            steps = steps,
            status = BreedingProgramStatus.ACTIVE,
            planSpecies = speciesEnum,
            finalGeneration = steps.maxOfOrNull { it.generation } ?: 1
        )
    }

    private fun formatSource(source: String): String {
        return when {
            source.startsWith("BIRD:") -> "Bird #${source.removePrefix("BIRD:")}"
            source.startsWith("BREED:") -> "${source.removePrefix("BREED:")} Breed"
            source.startsWith("SCENARIO:") -> "F${source.split(":").lastOrNull() ?: "?"} Offspring"
            else -> source
        }
    }
}

