package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.genetics.Certainty
import com.example.hatchtracker.domain.breeding.plan.PlanConstraints
import com.example.hatchtracker.model.genetics.GenotypeCall
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import com.example.hatchtracker.data.models.RoadmapStage
import com.example.hatchtracker.data.models.RoadmapStageType
import com.example.hatchtracker.data.models.BaseLineDefinition
import kotlinx.coroutines.yield
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

data class ScoreCacheKey(
    val sireId: String,
    val damId: String,
    val species: String
)

@Singleton
class StrategySearchEngine @Inject constructor(
    private val breedingPredictionService: BreedingPredictionService,
    private val planScorer: PlanScorer,
    private val goalEvaluationEngine: GoalEvaluationEngine,
    private val genotypePriorService: GenotypePriorService
) {

    private val predictionCache = ConcurrentHashMap<ScoreCacheKey, BreedingPredictionResult>()

    suspend fun search(
        species: Species,
        population: List<Bird>,
        template: BreedingGoalTemplate,
        config: SearchConfig,
        constraints: PlanConstraints = PlanConstraints(),
        roadmapContext: RoadmapConstraintContext? = null
    ): List<BreedingProgram> {
        predictionCache.clear()
        
        val males = population.filter { it.sex != Sex.FEMALE }
        val females = population.filter { it.sex != Sex.MALE }

        val beam = mutableListOf<BreedingProgram>()

        // Generation 1: Real birds only
        males.forEach { male ->
            females.forEach { female ->
                if (male.species != female.species) return@forEach
                
                val (plan, score) = evaluatePairWithScore(species, male, female, template, 1, population = population)
                
                // Strict Pruning Gen 1
                if (score.satisfiedRequired && score.matchPercentage >= 40.0) {
                    beam.add(plan)
                }
            }
        }

        // Stable sort
        beam.sortWith(compareByDescending<BreedingProgram> { it.overallScore }.thenBy { it.id })
        var currentTopPlans = beam.take(config.beamWidth)

        // Multi-generation expansion (Beam Search)
        val maxGen = minOf(config.maxGenerations, constraints.maxGenerations)
        
        for (generation in 2..maxGen) {
            yield() // Cooperative cancellation
            
            // Early Exit Check
            val topPlan = currentTopPlans.firstOrNull()
            if (topPlan != null) {
                val lastStability = topPlan.pathway.last().stability
                if (lastStability != null) {
                    val goalMatch = lastStability.components.confidence
                    val fixation = lastStability.components.fixation
                    if (goalMatch >= 0.95 && fixation >= 0.90) break
                }
            }

            val nextGenCandidates = mutableListOf<BreedingProgram>()
            
            currentTopPlans.forEach { existingPlan ->
                val lastGenNum = existingPlan.pathway.size
                val lastGenData = existingPlan.pathway.last()
                
                // Create Virtual Candidate Offspring (K=2)
                val virtualCandidates = createVirtualBirdsFromPlan(existingPlan, species, lastGenNum + 1)
                
                virtualCandidates.forEach { virtualOffspring ->
                    val currentPopPlusVirtual = population + virtualOffspring
                    
                    population.filter { it.species == species }.forEach { partner ->
                        if (!coroutineContext.isActive) return@forEach
                        
                        // Robust sex filtering: avoid UNKNOWN/UNKNOWN and same-sex
                        if (effectiveSex(virtualOffspring) == effectiveSex(partner) && effectiveSex(partner) != Sex.UNKNOWN) return@forEach
                        if (effectiveSex(virtualOffspring) == Sex.UNKNOWN && effectiveSex(partner) == Sex.UNKNOWN) return@forEach

                        // Respect sire reuse if constrained
                        if (constraints.limitSireReuse && effectiveSex(partner) == Sex.MALE) {
                            val reuseCount = existingPlan.pathway.count { it.sireId == partner.localId.toString() }
                            if (reuseCount >= 2) return@forEach
                        }

                        val sire = if (effectiveSex(virtualOffspring) == Sex.MALE) virtualOffspring else if (effectiveSex(partner) == Sex.MALE) partner else null
                        val dam = if (effectiveSex(virtualOffspring) == Sex.FEMALE) virtualOffspring else if (effectiveSex(partner) == Sex.FEMALE) partner else null
                        
                        if (sire != null && dam != null) {
                            val (nextStepResult, nextScore) = evaluatePairWithScore(
                                species = species,
                                sire = sire,
                                dam = dam,
                                template = template,
                                generation = lastGenNum + 1,
                                previousStability = lastGenData.stability,
                                population = currentPopPlusVirtual
                            )
                            
                            // Pruning: Required satisfaction is non-negotiable for future gens
                            // Also prune if roadmap constraints (donor fraction/type) are violated
                            if (!nextScore.satisfiedRequired || nextScore.matchPercentage < 40.0) return@forEach

                            if (roadmapContext != null && !validateAgainstRoadmap(sire, dam, roadmapContext, generation)) {
                                return@forEach
                            }

                            val expandedPlan = existingPlan.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                overallScore = (existingPlan.overallScore * 0.4 + nextStepResult.overallScore * 0.6),
                                pathway = existingPlan.pathway + nextStepResult.pathway
                            )
                            nextGenCandidates.add(expandedPlan)
                        }
                    }
                }
            }
            
            if (nextGenCandidates.isEmpty()) break
            currentTopPlans = nextGenCandidates.sortedWith(compareByDescending<BreedingProgram> { it.overallScore }.thenBy { it.id }).take(config.beamWidth)
        }
        
        return currentTopPlans.sortedWith(compareByDescending<BreedingProgram> { it.overallScore }.thenBy { it.id }).take(config.topKPlans)
    }

    private fun effectiveSex(bird: Bird): Sex {
        return if (bird.sex == Sex.UNKNOWN) {
            // Predict sex based on Z-linked loci if available, or just use ID parity
            if (bird.localId % 2L == 0L) Sex.MALE else Sex.FEMALE
        } else bird.sex
    }

    private fun createVirtualBirdsFromPlan(plan: BreedingProgram, species: Species, gen: Int): List<Bird> {
        val lastStep = plan.pathway.last()
        return lastStep.candidateKeepers.mapIndexed { index, genotypes ->
            Bird(
                localId = -(System.currentTimeMillis() % 1000000) - (gen * 10) - index,
                syncId = "v_offspring_${plan.id}_${gen}_$index",
                species = species,
                breed = "Virtual F${gen-1} Offspring",
                sex = if ((gen + index) % 2 == 0) Sex.MALE else Sex.FEMALE,
                hatchDate = "TBD",
                geneticProfile = com.example.hatchtracker.model.GeneticProfile(
                    genotypeCalls = genotypes,
                    fixedTraits = lastStep.expectedTraitGains,
                    confidenceLevel = com.example.hatchtracker.data.models.ConfidenceLevel.MEDIUM.name
                )
            )
        }
    }

    private fun evaluatePairWithScore(
        species: Species,
        sire: Bird,
        dam: Bird,
        template: BreedingGoalTemplate,
        generation: Int,
        previousStability: LineStabilitySnapshot? = null,
        population: List<Bird> = emptyList()
    ): Pair<BreedingProgram, GoalScore> {
        val sireProfile = genotypePriorService.buildPriors(species, sire.geneticProfile)
        val damProfile = genotypePriorService.buildPriors(species, dam.geneticProfile)

        val pairKey = ScoreCacheKey(sire.syncId, dam.syncId, species.name)
        val prediction = predictionCache.getOrPut(pairKey) {
            breedingPredictionService.predictBreeding(
                species = species,
                sireProfile = sireProfile,
                damProfile = damProfile
            )
        }

        val goalScore = goalEvaluationEngine.evaluate(
            prediction,
            template.mustHave,
            template.niceToHave,
            template.avoid
        )

        val risk = GeneticRiskAnalyzer.analyzeBreedingRisk(sire, dam, population)
        
        val diversityMeta = DiversityMeta(
            uniqueSires = population.mapNotNull { it.fatherId }.distinct().size,
            repeatedSireCount = if (sire.localId != 0L && population.any { it.fatherId == sire.localId }) 1 else 0,
            totalBirds = population.size
        )

        val stability = breedingPredictionService.calculateLineStability(
            species = species,
            generationIndex = generation,
            prediction = prediction,
            goal = template.mustHave.firstOrNull(),
            risk = risk,
            diversityMeta = diversityMeta,
            previous = previousStability
        )

        val scoreResult = planScorer.scorePlan(
            goalMatches = (goalScore.matchPercentage / 100.0).coerceIn(0.0, 1.0),
            inbreedingRisk = risk.inbreedingRiskScore,
            bottleneckRisk = risk.bottleneckRiskScore,
            uncertainty = calculateUncertainty(sire.geneticProfile, dam.geneticProfile),
            diversityBonus = template.diversityWeight
        )

        val gains = analyzeQualitativeTraits(prediction)
        
        // Select keeper genotypes for the virtual bird
        val keeperCandidates = selectKeeperGenotypes(
            prediction = prediction, 
            template = template,
            preferHeterozygous = template.diversityWeight > 0.6
        )

        val genData = ProgramGeneration(
            generationNumber = generation,
            milestoneGoal = template.title,
            maleSource = sire.breed,
            femaleSource = dam.breed,
            sireId = sire.localId.toString(),
            damId = dam.localId.toString(),
            isVirtual = sire.localId < 0 || dam.localId < 0,
            virtualSyncId = if (sire.localId < 0) sire.syncId else if (dam.localId < 0) dam.syncId else null,
            expectedTraitGains = gains,
            selectionGuidance = generateSelectionGuidance(stability, template),
            retentionCriteria = generateRetentionCriteria(stability, prediction),
            stability = stability,
            candidateKeepers = keeperCandidates
        )

        val pathway = if (stability.isEstablished) {
            listOf(genData, createRefinementStep(generation + 1, stability))
        } else {
            listOf(genData)
        }

        val plan = BreedingProgram(
            id = java.util.UUID.randomUUID().toString(),
            sireId = sire.localId.toString(),
            damId = dam.localId.toString(),
            overallScore = scoreResult.totalScore.toDouble(),
            summaryRationale = stability.banner?.title ?: if (stability.isEstablished) "Bio-stability achieved." else "Line analysis complete.",
            pathway = pathway
        )
        
        return plan to goalScore
    }

    private fun selectKeeperGenotypes(
        prediction: BreedingPredictionResult,
        template: BreedingGoalTemplate,
        preferHeterozygous: Boolean = false
    ): List<Map<String, GenotypeCall>> {
        val candidateGenotypesPool = mutableListOf<Map<String, GenotypeCall>>()
        
        // We want to generate K=2 distinct virtual birds if possible.
        // We'll perform two selection passes.
        
        val passes = if (preferHeterozygous) 1 else 2 // If maximizing diversity, maybe we just want variance?
        
        repeat(passes) { passIndex ->
            val keeper = mutableMapOf<String, GenotypeCall>()
            template.mustHave.forEach { target ->
                val dist = prediction.offspringDistributions[target.traitId] ?: return@forEach
                
                val sortedOutcomes = dist.outcomes.entries
                    .filter { it.key.alleles.contains(target.valueId) }
                    .sortedByDescending { outcome ->
                        var selectionWeight = outcome.value // probability
                        
                        val isHomozygous = outcome.key.alleles.size == 2 && outcome.key.alleles[0] == outcome.key.alleles[1]
                        
                        if (!preferHeterozygous) {
                            if (isHomozygous) selectionWeight *= 1.5 // Prefer homozygous for stabilization
                        } else {
                            if (!isHomozygous) selectionWeight *= 1.2 // Prefer heterozygous for diversity
                        }
                        selectionWeight
                    }
                
                val chosenOutcome = if (passIndex < sortedOutcomes.size) {
                    sortedOutcomes[passIndex].key
                } else {
                    sortedOutcomes.firstOrNull()?.key
                }

                if (chosenOutcome != null) {
                    keeper[target.traitId] = GenotypeCall(
                        locusId = target.traitId,
                        alleles = chosenOutcome.alleles,
                        certainty = com.example.hatchtracker.model.genetics.Certainty.ASSUMED
                    )
                }
            }
            if (keeper.isNotEmpty()) {
                candidateGenotypesPool.add(keeper)
            }
        }
        
        return if (candidateGenotypesPool.isEmpty()) emptyList() else candidateGenotypesPool.distinct()
    }

    private fun calculateUncertainty(sire: GeneticProfile, dam: GeneticProfile): Double {
        val allCalls = (sire.genotypeCalls?.values ?: emptyList()) + 
                       (dam.genotypeCalls?.values ?: emptyList())
        if (allCalls.isEmpty()) return 1.0
        val avgCertainty = allCalls.map { call ->
            when(call.certainty) {
                Certainty.CONFIRMED -> 1.0
                Certainty.ASSUMED -> 0.6
                else -> 0.3
            }
        }.average()
        return (1.0 - avgCertainty).coerceIn(0.0, 1.0)
    }

    private fun analyzeQualitativeTraits(prediction: BreedingPredictionResult): List<String> {
        return prediction.phenotypeOutcomes.filter { it.overallProbability > 0.5 }.map { it.label }
    }

    private fun generateSelectionGuidance(stability: LineStabilitySnapshot, template: BreedingGoalTemplate): String {
        return when {
            stability.glsiScore < 40 -> "Primary Focus: Identification. Select only offspring with confirmed phenotypes matching ${template.title}."
            stability.components.risk < 0.5 -> "Caution: High inbreeding. Select for vigor and introduce unrelated stock if possible."
            stability.components.fixation < 0.6 -> "Stabilization phase: Select homozygous individuals for ${template.mustHave.firstOrNull()?.traitId ?: "primary traits"}."
            else -> "Refinement phase: Select for subtle improvements in size, type, or secondary traits."
        }
    }

    private fun generateRetentionCriteria(stability: LineStabilitySnapshot, prediction: BreedingPredictionResult): String {
        return when {
            stability.glsiScore < 40 -> "Retain only offspring that visibly express the target traits."
            prediction.phenotypeOutcomes.any { it.overallProbability >= 0.8 } ->
                "Prioritize offspring from the highest-probability trait outcomes."
            else -> "High consistency: retain based on overall health and vitality."
        }
    }
    private fun createRefinementStep(generation: Int, stability: LineStabilitySnapshot): ProgramGeneration {
        return ProgramGeneration(
            generationNumber = generation,
            milestoneGoal = "Line Refinement & Consistency Testing",
            maleSource = "Established Line",
            femaleSource = "Established Line",
            isVirtual = true,
            selectionGuidance = "The line is now genetically established. Focus on refining secondary characteristics (size, type, plumage quality) while maintaining homozygous fixation of primary traits.",
            retentionCriteria = "Retain only individuals that match the established standard perfectly. Cull any remaining segregations.",
            stability = stability
        )
    }

    private fun validateAgainstRoadmap(sire: Bird, dam: Bird, context: RoadmapConstraintContext, generation: Int): Boolean {
        val currentStage = context.stages.find { it.stageIndex == generation } ?: return true

        // 1. Check Source Alignment (Abstract Lineage)
        if (currentStage.type == RoadmapStageType.BACKCROSS) {
            val hasBaseLine = (sire.breed == context.baseLine.baseBreed || dam.breed == context.baseLine.baseBreed)
            if (!hasBaseLine) return false
        }

        // 2. Check Donor Fraction (Heuristic approximation)
        val sireDonFraction = if (sire.breed == context.baseLine.baseBreed) 0.0 else context.currentDonorFraction
        val damDonFraction = if (dam.breed == context.baseLine.baseBreed) 0.0 else context.currentDonorFraction
        val nextFraction = (sireDonFraction + damDonFraction) / 2.0

        if (nextFraction > currentStage.donorFractionAfter + 0.1) return false

        return true
    }
}

data class RoadmapConstraintContext(
    val baseLine: BaseLineDefinition,
    val stages: List<RoadmapStage>,
    val currentDonorFraction: Double = 0.5
)

