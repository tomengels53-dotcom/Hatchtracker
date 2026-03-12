package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.model.BreedStandard
import com.example.hatchtracker.model.Species
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StagedCrossArchitect @Inject constructor(
    private val generationEstimator: GenerationEstimator,
    private val breedingFacade: BreedingFacade
) {

    /**
     * Scores a potential donor breed based on target traits and compatibility.
     */
    fun scoreDonor(
        breed: BreedStandard,
        targetTraits: List<GoalSpec>,
        mode: StrategyMode
    ): Double {
        var weightedSum = 0.0
        var matchingTraits = 0
        var dominantAcquisitions = 0

        targetTraits.forEach { goal ->
            val hasTrait = breed.geneticProfile.knownGenes.contains(goal.targetValue) ||
                           breed.geneticProfile.fixedTraits.contains(goal.targetValue) ||
                           breed.geneticProfile.inferredTraits.contains(goal.targetValue)
            
            if (!hasTrait) return@forEach
            
            // Trait Match Weight (0.1 to 0.4)
            // Assuming 1.0 reliability for standard fixed traits for MVP scoring
            val reliability = 1.0
            val matchWeight = (goal.priority / 5.0) * reliability * 0.4
            weightedSum += matchWeight
            matchingTraits++

            // Track dominant trait acquisitions for overload penalty
            if (reliability > 0.7) {
                dominantAcquisitions++
            }
        }

        if (matchingTraits == 0) return 0.0

        // Trait Overload Penalty (STRICT mode only)
        if (mode == StrategyMode.STRICT_LINE_BREEDING && dominantAcquisitions > 2) {
            val overloadPenalty = (dominantAcquisitions - 2) * 0.15
            weightedSum -= overloadPenalty
        }

        // Dilution Penalty (Identity Preservation)
        val dilutionPenalty = if (mode == StrategyMode.STRICT_LINE_BREEDING) 0.2 else 0.05
        weightedSum -= dilutionPenalty

        return weightedSum.coerceAtLeast(0.0)
    }

    /**
     * Generates a deterministic roadmap based on base population and goals.
     */
    fun createRoadmap(
        species: Species,
        baseLine: BaseLineDefinition,
        goals: List<GoalSpec>,
        mode: StrategyMode
    ): MultiBreedRoadmap {
        val stages = mutableListOf<RoadmapStage>()
        val deficits = goals.filter { it.priority >= 3 } // Consider only high-mid priority traits for roadmap
        
        // Find best donor for the most complex/priority deficit
        val allBreeds = breedingFacade.getBreedsForSpecies(species.name)
        val bestDonor = deficits.mapNotNull { goal ->
            allBreeds.filter { breed -> 
                breed.geneticProfile.knownGenes.contains(goal.targetValue) || 
                breed.geneticProfile.fixedTraits.contains(goal.targetValue) ||
                breed.geneticProfile.inferredTraits.contains(goal.targetValue)
            }
            .map { it to scoreDonor(it, deficits, mode) }
            .maxByOrNull { it.second }
        }.maxByOrNull { it.second }?.first

        var currentDonorFraction = 0.0
        var stageIndexCounter = 1

        val toleranceThreshold = if (mode == StrategyMode.STRICT_LINE_BREEDING) 0.05 else 0.50

        // Stage 1: INTROGRESS
        if (bestDonor != null) {
            val beforeFrac = 1.0
            currentDonorFraction = 0.5
            val stageGen = estimateStage(RoadmapStageType.INTROGRESS, deficits, currentDonorFraction)
            stages.add(
                RoadmapStage(
                    stageIndex = stageIndexCounter++,
                    type = RoadmapStageType.INTROGRESS,
                    targetTraits = deficits.map { it.traitKey },
                    sireSource = bestDonor.name,
                    damSource = "Base Line",
                    selectionRules = listOf("Select F1 expressers/carriers", "Prioritize vigor"),
                    expectedOutcome = "F1 hybrids carrying target donor traits.",
                    whyThisStage = "Initial acquisition of traits not currently present in base population. Donor fraction: ${(beforeFrac*100).toInt()}% -> ${(currentDonorFraction*100).toInt()}%.",
                    genEstimate = stageGen,
                    donorFractionBefore = beforeFrac,
                    donorFractionAfter = currentDonorFraction
                )
            )
        }

        if (mode == StrategyMode.STRICT_LINE_BREEDING) {
            // BACKCROSS Loop
            if (bestDonor != null) {
                val requiredBackcrosses = kotlin.math.ceil(kotlin.math.log2(0.5 / toleranceThreshold)).toInt().coerceIn(0, 6)
                for (i in 1..requiredBackcrosses) {
                    val beforeFrac = currentDonorFraction
                    currentDonorFraction /= 2.0
                    val bcGen = estimateStage(RoadmapStageType.BACKCROSS, deficits, currentDonorFraction)
                    stages.add(
                        RoadmapStage(
                            stageIndex = stageIndexCounter++,
                            type = RoadmapStageType.BACKCROSS,
                            targetTraits = deficits.map { it.traitKey },
                            sireSource = "Base Line",
                            damSource = "Previous Gen",
                            selectionRules = listOf("Select for phenotypic recovery of base breed", "Verify trait carriage"),
                            expectedOutcome = "BC$i offspring with high base-breed phenotype recovery.",
                            whyThisStage = "Backcross required to reduce donor genome from ${String.format("%.1f", beforeFrac*100)}% to ${String.format("%.1f", currentDonorFraction*100)}%.",
                            genEstimate = bcGen,
                            donorFractionBefore = beforeFrac,
                            donorFractionAfter = currentDonorFraction
                        )
                    )
                }
            }

            // Final Stage: FIXATION
            val fixGen = estimateStage(RoadmapStageType.FIXATION, deficits, currentDonorFraction)
            stages.add(
                RoadmapStage(
                    stageIndex = stageIndexCounter++,
                    type = RoadmapStageType.FIXATION,
                    targetTraits = deficits.map { it.traitKey },
                    sireSource = "Previous Gen",
                    damSource = "Previous Gen",
                    selectionRules = listOf("Select homozygous individuals", "Cull any segregants"),
                    expectedOutcome = "Fixed, true-breeding line.",
                    whyThisStage = "Ensuring genetic stability of target traits.",
                    genEstimate = fixGen,
                    donorFractionBefore = currentDonorFraction,
                    donorFractionAfter = currentDonorFraction
                )
            )

        } else if (mode == StrategyMode.COMMERCIAL_PRODUCTION) {
            if (bestDonor != null) {
                // INTERCROSS
                val beforeFrac = currentDonorFraction
                val icGen = estimateStage(RoadmapStageType.INTERCROSS, deficits, currentDonorFraction)
                stages.add(
                    RoadmapStage(
                        stageIndex = stageIndexCounter++,
                        type = RoadmapStageType.INTERCROSS,
                        targetTraits = deficits.map { it.traitKey },
                        sireSource = "Previous Gen",
                        damSource = "Previous Gen",
                        selectionRules = listOf("Select top 10% performance performers", "Maximize heterosis"),
                        expectedOutcome = "F2 segregation allowing selection for performance combinations.",
                        whyThisStage = "Increasing genetic variance to find elite performers.",
                        genEstimate = icGen,
                        donorFractionBefore = beforeFrac,
                        donorFractionAfter = currentDonorFraction
                    )
                )
            }

            // Final Stage: STABILIZE
            val finalGen = estimateStage(RoadmapStageType.STABILIZE, deficits, currentDonorFraction)
            stages.add(
                RoadmapStage(
                    stageIndex = stageIndexCounter++,
                    type = RoadmapStageType.STABILIZE,
                    targetTraits = deficits.map { it.traitKey },
                    sireSource = "Previous Gen",
                    damSource = "Previous Gen",
                    selectionRules = listOf("Maintain consistent performance levels", "Avoid narrowing genetic base"),
                    expectedOutcome = "Stabilized performance population.",
                    whyThisStage = "Locking in hybrid vigor and productivity.",
                    genEstimate = finalGen,
                    donorFractionBefore = currentDonorFraction,
                    donorFractionAfter = currentDonorFraction
                )
            )
        }

        return MultiBreedRoadmap(
            id = UUID.randomUUID().toString(),
            baseLine = baseLine,
            stages = stages,
            overallGenEstimate = generationEstimator.aggregate(stages.map { it.genEstimate })
        )
    }

    private fun estimateStage(type: RoadmapStageType, goals: List<GoalSpec>, donorFraction: Double): GenEstimate {
        val quantitativeTraitKeys = setOf("TEMPERAMENT", "WEIGHT", "SIZE", "EGG_PRODUCTION", "GROWTH_RATE")
        val complexities = goals.map { goal ->
            if (quantitativeTraitKeys.contains(goal.traitKey.uppercase())) {
                GeneticComplexity.POLYGENIC
            } else {
                GeneticComplexity.MONOGENIC
            }
        }
        return generationEstimator.estimateForStage(type, complexities, donorFraction)
    }
}

