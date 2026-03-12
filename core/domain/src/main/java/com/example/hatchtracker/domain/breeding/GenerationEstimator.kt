package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.GoalSpec
import com.example.hatchtracker.data.models.StartingSituation
import com.example.hatchtracker.data.models.StrategyConfig
import com.example.hatchtracker.data.models.StrategyMode
import com.example.hatchtracker.data.models.TraitDomain
import com.example.hatchtracker.data.models.GenEstimate
import com.example.hatchtracker.data.models.EstimateConfidence

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerationEstimator @Inject constructor() {

    fun estimate(
        config: StrategyConfig,
        observedExpression: Double = 0.0,
        sampleSize: Int = 0
    ): GenEstimate {
        // Base estimation logic preserved for non-staged plans
        val complexityScore = config.goalSpecs.sumOf { calculateGoalComplexity(it) }
        
        var minGens = 1
        var maxGens = 3

        when (config.startingSituation) {
            StartingSituation.COMBINE_FLOCKS -> { minGens += 1; maxGens += 2 }
            StartingSituation.IMPROVE_FLOCK -> { minGens += 2; maxGens += 4 }
            StartingSituation.START_FROM_SCRATCH -> { minGens += 3; maxGens += 6 }
        }

        val genBump = (complexityScore / 10).toInt()
        minGens += genBump
        maxGens += (genBump * 2)

        if (config.strategyMode == StrategyMode.STRICT_LINE_BREEDING) {
            maxGens += 2
        }

        val limitingFactors = mutableListOf<String>()
        if (sampleSize > 0) {
            if (sampleSize < 5) {
                limitingFactors.add("Low sample size increases estimate uncertainty")
                maxGens += 1
            } else if (observedExpression < 0.1) {
                limitingFactors.add("Target traits not appearing in offspring")
                minGens += 1
                maxGens += 2
            }
        }

        var hasStrongNegativeCovariance = false
        val allTraitKeys = config.goalSpecs.map { it.traitKey }
        for (i in allTraitKeys.indices) {
            for (j in i + 1 until allTraitKeys.size) {
                if (TraitCovarianceCatalog.getCorrelation(allTraitKeys[i], allTraitKeys[j]) < -0.5) {
                    hasStrongNegativeCovariance = true
                }
            }
        }

        if (hasStrongNegativeCovariance) {
            maxGens += 1
            limitingFactors.add("Strong negative trait correlations present")
        }

        var confScore = if (complexityScore > 20) 0.5 else 0.8
        
        val Cm = confScore 
        val covariancePenalty = if (hasStrongNegativeCovariance) 0.2 else 0.0
        val Cp = (1.0 - covariancePenalty).coerceIn(0.1, 1.0)
        
        var Ce = 1.0
        if (sampleSize > 0) {
            Ce = if (sampleSize < 5) 0.6 else if (observedExpression < 0.1) 0.4 else 0.9
        }
        
        var cTotal = java.lang.Math.pow(Ce * Cm * Cp, 1.0 / 3.0)
        cTotal = cTotal.coerceIn(0.35, 0.98)
        
        val confEnum = when {
            cTotal > 0.8 -> EstimateConfidence.HIGH
            cTotal > 0.5 -> EstimateConfidence.MED
            else -> EstimateConfidence.LOW
        }

        val breakdown = com.example.hatchtracker.data.models.ConfidenceBreakdown(
            evidence = Ce,
            model = Cm,
            planComplexity = Cp,
            topRiskFactors = limitingFactors.toList().take(3)
        )

        return GenEstimate(
            minGenerations = minGens,
            maxGenerations = maxGens,
            confidence = confEnum,
            limitingFactors = limitingFactors,
            confidenceBreakdown = breakdown
        )
    }

    /**
     * Estimates generations for a specific Roadmap Stage type.
     */
    fun estimateForStage(
        type: com.example.hatchtracker.data.models.RoadmapStageType,
        traitComplexities: List<com.example.hatchtracker.data.models.GeneticComplexity>,
        donorFraction: Double = 0.0
    ): GenEstimate {
        var min = 1
        var max = 1
        var confidence = EstimateConfidence.HIGH
        val factors = mutableListOf<String>()

        val polygenicCount = traitComplexities.count { it == com.example.hatchtracker.data.models.GeneticComplexity.POLYGENIC }

        when (type) {
            com.example.hatchtracker.data.models.RoadmapStageType.INTROGRESS -> {
                // F1 is always 1 gen
            }
            com.example.hatchtracker.data.models.RoadmapStageType.BACKCROSS -> {
                max = 2
                confidence = EstimateConfidence.MED
                factors.add("Phenotypic recovery timing varies")
            }
            com.example.hatchtracker.data.models.RoadmapStageType.INTERCROSS -> {
                max = 3
                confidence = EstimateConfidence.LOW
                factors.add("Mendelian segregation is probabilistic")
            }
            com.example.hatchtracker.data.models.RoadmapStageType.FIXATION -> {
                min = 2
                max = 4 + polygenicCount
                confidence = EstimateConfidence.MED

                if (polygenicCount > 0) {
                    factors.add("Polygenic traits require extended selection for fixation ($polygenicCount quantitative traits)")
                    confidence = EstimateConfidence.LOW
                    min += 1 // Strict mode fixation bump for quantitative traits
                    max += (polygenicCount * 2) - polygenicCount // Adds +1 to +2 to maxGenerations based on count
                }
                
                if (donorFraction > 0.02) {
                    min += 1
                }
                
                factors.add("Fixation requires isolating target traits")
            }
            com.example.hatchtracker.data.models.RoadmapStageType.STABILIZE -> {
                max = 2
                factors.add("Focus on performance consistency")
            }
        }

        return GenEstimate(min, max, confidence, factors)
    }

    /**
     * Aggregates multiple stage estimates into a conservative roadmap estimate.
     */
    fun aggregate(
        stages: List<GenEstimate>,
        evidenceConfidence: Double = 1.0,
        covariancePenalty: Double = 0.0,
        donorCount: Int = 1
    ): GenEstimate {
        if (stages.isEmpty()) return GenEstimate(0, 0, EstimateConfidence.HIGH, emptyList())

        var minTotal = 0
        var maxTotal = 0
        var modelConfidenceSum = 0.0
        val allFactors = mutableListOf<String>()

        stages.forEach { stage ->
            minTotal += stage.minGenerations
            maxTotal += stage.maxGenerations
            allFactors.addAll(stage.limitingFactors)
            
            val stageConf = when(stage.confidence) {
                EstimateConfidence.HIGH -> 0.95
                EstimateConfidence.MED -> 0.70
                EstimateConfidence.LOW -> 0.40
            }
            modelConfidenceSum += stageConf
        }
        
        val Cm = modelConfidenceSum / stages.size
        
        val stagePenalty = (stages.size - 1) * 0.05
        val donorPenalty = (donorCount - 1) * 0.1
        var Cp = 1.0 - stagePenalty - donorPenalty - covariancePenalty
        Cp = Cp.coerceIn(0.1, 1.0)
        
        val Ce = evidenceConfidence
        
        var cTotal = java.lang.Math.pow(Ce * Cm * Cp, 1.0 / 3.0)
        cTotal = cTotal.coerceIn(0.35, 0.98)

        val finalConfidence = when {
            cTotal > 0.8 -> EstimateConfidence.HIGH
            cTotal > 0.5 -> EstimateConfidence.MED
            else -> EstimateConfidence.LOW
        }

        val breakdown = com.example.hatchtracker.data.models.ConfidenceBreakdown(
            evidence = Ce,
            model = Cm,
            planComplexity = Cp,
            topRiskFactors = allFactors.distinct().take(3)
        )

        return GenEstimate(
            minGenerations = minTotal,
            maxGenerations = maxTotal,
            confidence = finalConfidence,
            limitingFactors = allFactors.distinct(),
            confidenceBreakdown = breakdown
        )
    }

    private fun calculateGoalComplexity(goal: GoalSpec): Double {
        // Heuristic mapping of traits to complexity
        return when (goal.domain) {
            TraitDomain.GENETIC_STABILITY -> 8.0
            TraitDomain.PRODUCTIVITY -> 6.0
            TraitDomain.PLUMAGE_TRAITS -> 5.0
            TraitDomain.EGG_TRAITS -> 4.0
            TraitDomain.BEHAVIOR -> 3.0
            TraitDomain.STRUCTURAL_TRAITS -> 3.0
        } * (goal.priority / 3.0)
    }
}
