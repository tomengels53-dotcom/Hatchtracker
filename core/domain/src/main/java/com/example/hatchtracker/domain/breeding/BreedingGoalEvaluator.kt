package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingTarget
import com.example.hatchtracker.data.models.TargetSex
import com.example.hatchtracker.data.models.TraitTarget
import com.example.hatchtracker.model.genetics.GenotypeCall
import com.example.hatchtracker.model.genetics.GenotypeDistribution
import kotlin.math.min

data class GoalScore(
    val totalScore: Double,
    val matchPercentage: Double,
    val satisfiedRequired: Boolean,
    val stabilityIndex: Double,
    val breakdown: Map<String, Double>
)

class BreedingGoalEvaluator {

    companion object {
        private const val REQUIRED_WEIGHT = 3.0
        private const val STABILITY_WEIGHT = 0.15
        private const val HARD_PENALTY_MULTIPLIER = 0.2
        private const val MAX_SCORE_BASE = 1000.0 // Normalization base
        private const val REQUIRED_MIN_PROB = 0.65
        private const val AVOID_MAX_PROB = 0.30
        private const val AVOID_PENALTY_MULTIPLIER = 0.7
    }

    fun evaluate(
        outcomes: List<PhenotypeOutcome>,
        quantitativePredictions: List<QuantitativePrediction>,
        offspringDistributions: Map<String, GenotypeDistribution>,
        target: BreedingTarget
    ): GoalScore {
        val maleRatio = target.maleRatio.coerceIn(0.0, 1.0)
        val femaleRatio = target.femaleRatio.coerceIn(0.0, 1.0)
        
        // Normalize ratios if they don't sum to 1
        val sum = maleRatio + femaleRatio
        val (adjMale, adjFemale) = if (sum > 0) {
            maleRatio / sum to femaleRatio / sum
        } else {
            0.5 to 0.5
        }

        var requiredScore = 0.0
        var satisfiedRequired = true
        var preferredScore = 0.0
        var penalty = 0.0
        val breakdown = mutableMapOf<String, Double>()

        // 1. REQUIRED
        target.requiredTraits.forEach { traitTarget ->
            val quantPred = quantitativePredictions.find { it.traitKey == traitTarget.traitId }
            if (quantPred != null) {
                val targetMean = traitTarget.valueId.toDoubleOrNull() ?: 0.5
                val p = 1.0 - kotlin.math.abs(quantPred.mean - targetMean)
                if (p < REQUIRED_MIN_PROB) satisfiedRequired = false
                val score = (p * p) * 100 * REQUIRED_WEIGHT
                requiredScore += score
                breakdown["REQUIRED_${traitTarget.traitId}"] = score
            } else {
                val p = getSexAwareProbability(outcomes, traitTarget, adjMale, adjFemale)
                if (p < REQUIRED_MIN_PROB) satisfiedRequired = false
                
                val score = (p * p) * 100 * REQUIRED_WEIGHT
                requiredScore += score
                breakdown["REQUIRED_${traitTarget.traitId}"] = score
            }
        }

        // 2. PREFERRED
        target.preferredTraits.forEach { traitTarget ->
            val quantPred = quantitativePredictions.find { it.traitKey == traitTarget.traitId }
            val p = if (quantPred != null) {
                val targetMean = traitTarget.valueId.toDoubleOrNull() ?: 0.5
                1.0 - kotlin.math.abs(quantPred.mean - targetMean)
            } else {
                getSexAwareProbability(outcomes, traitTarget, adjMale, adjFemale)
            }
            val score = (p * p) * 100 * traitTarget.weight
            preferredScore += score
            breakdown["PREFERRED_${traitTarget.traitId}"] = score
        }

        // 3. EXCLUDED
        var hardAvoidHit = false
        target.excludedTraits.forEach { traitTarget ->
            val p = getSexAwareProbability(outcomes, traitTarget, adjMale, adjFemale)
            if (p >= AVOID_MAX_PROB) hardAvoidHit = true
            val score = (p * p) * 100 * traitTarget.weight
            penalty += score
            breakdown["EXCLUDED_${traitTarget.traitId}"] = -score
        }

        // 4. Base Score
        var totalScore = requiredScore + preferredScore - penalty

        // 5. Hard Penalty for missing required
        if (!satisfiedRequired) {
            totalScore *= HARD_PENALTY_MULTIPLIER
        }

        // 5b. Hard Penalty for hitting avoid max
        if (hardAvoidHit) {
            totalScore *= AVOID_PENALTY_MULTIPLIER
        }

        // 6. Stability Penalty (Heterozygous check)
        var heterozygousProbabilitySum = 0.0
        val requiredLoci = target.requiredTraits.map { it.traitId }.toSet()
        
        requiredLoci.forEach { locusId ->
            val dist = offspringDistributions[locusId]
            if (dist != null) {
                val heteroProb = dist.outcomes.entries.sumOf { (genotype, prob) ->
                    if (genotype.alleles.size == 2 && genotype.alleles[0] != genotype.alleles[1]) prob else 0.0
                }
                heterozygousProbabilitySum += heteroProb
            }
        }
        
        if (requiredLoci.isNotEmpty()) {
            val avgHeterozygousProb = heterozygousProbabilitySum / requiredLoci.size
            totalScore *= (1.0 - STABILITY_WEIGHT * avgHeterozygousProb)
        }

        // 7. Stability Index (avg prob of required)
        val stabilityIndex = if (target.requiredTraits.isNotEmpty()) {
            target.requiredTraits.sumOf { traitTarget ->
                val quantPred = quantitativePredictions.find { it.traitKey == traitTarget.traitId }
                if (quantPred != null) {
                    val targetMean = traitTarget.valueId.toDoubleOrNull() ?: 0.5
                    1.0 - kotlin.math.abs(quantPred.mean - targetMean)
                } else {
                    getSexAwareProbability(outcomes, traitTarget, adjMale, adjFemale)
                }
            } / target.requiredTraits.size
        } else {
            1.0
        }

        // --- TRAIT COVARIANCE MODELING ---
        val lambda = 0.15
        val mu = 0.2
        var conflictPenaltySum = 0.0
        val inflatedVariances = mutableMapOf<String, Double>()

        val allQuantTargets = (target.requiredTraits.map { it to REQUIRED_WEIGHT } + 
                               target.preferredTraits.map { it to it.weight })
                              .filter { (t, _) -> quantitativePredictions.any { q -> q.traitKey == t.traitId } }

        allQuantTargets.forEach { (traitI, weightI) ->
            var sumAbsRho = 0.0
            val quantPredI = quantitativePredictions.find { it.traitKey == traitI.traitId } ?: return@forEach
            
            allQuantTargets.forEach { (traitJ, weightJ) ->
                if (traitI.traitId != traitJ.traitId) {
                    val rhoIj = TraitCovarianceCatalog.getCorrelation(traitI.traitId, traitJ.traitId)
                    sumAbsRho += kotlin.math.abs(rhoIj)
                    if (rhoIj < 0) {
                        conflictPenaltySum += (-rhoIj) * weightI * weightJ
                    }
                }
            }
            
            val varEff = quantPredI.variance * (1.0 + lambda * sumAbsRho)
            inflatedVariances[traitI.traitId] = varEff
        }
        
        // Divide by 2 to prevent double counting unique pairs
        val conflictPenaltyRaw = mu * (conflictPenaltySum / 2.0)
        
        // Scale to 100 base
        val scaledConflictPenalty = conflictPenaltyRaw * 100.0
        totalScore = kotlin.math.max(0.0, totalScore - scaledConflictPenalty)

        // quantitative traits variance penalties (now using inflated variances)
        var maxVariancePenalty = 0.0
        target.requiredTraits.forEach { traitTarget ->
            val quantPred = quantitativePredictions.find { it.traitKey == traitTarget.traitId }
            if (quantPred != null) {
                val varEff = inflatedVariances[traitTarget.traitId] ?: quantPred.variance
                val variancePenalty = min(varEff * 0.5, 0.3)
                if (variancePenalty > maxVariancePenalty) {
                    maxVariancePenalty = variancePenalty
                }
            }
        }
        
        var matchPercentage = (totalScore / calculateMaxPossibleScore(target) * 100).coerceIn(0.0, 100.0)
        
        // Multiply overall confidence/score match by (1 - min(conflictPenalty, 0.25)) and maxVariancePenalty
        val combinedConfidencePenalty = min(conflictPenaltyRaw, 0.25) + maxVariancePenalty
        if (combinedConfidencePenalty > 0) {
             matchPercentage = kotlin.math.max(35.0, matchPercentage * (1.0 - combinedConfidencePenalty))
        }

        return GoalScore(
            totalScore = totalScore,
            matchPercentage = matchPercentage,
            satisfiedRequired = satisfiedRequired,
            stabilityIndex = stabilityIndex,
            breakdown = breakdown
        )
    }

    private fun getSexAwareProbability(
        outcomes: List<PhenotypeOutcome>,
        target: TraitTarget,
        maleRatio: Double,
        femaleRatio: Double
    ): Double {
        val outcome = outcomes.firstOrNull { it.traitId == target.traitId && it.valueId == target.valueId }
            ?: return 0.0

        return when (target.appliesToSex) {
            TargetSex.FEMALE -> outcome.femaleProbability ?: outcome.overallProbability
            TargetSex.MALE -> outcome.maleProbability ?: outcome.overallProbability
            TargetSex.BOTH -> {
                if (outcome.maleProbability != null && outcome.femaleProbability != null) {
                    outcome.maleProbability * maleRatio + outcome.femaleProbability * femaleRatio
                } else {
                    outcome.overallProbability
                }
            }
        }
    }

    private fun calculateMaxPossibleScore(target: BreedingTarget): Double {
        // Deterministic max score for normalization
        val requiredMax = target.requiredTraits.size * 100.0 * REQUIRED_WEIGHT
        val preferredMax = target.preferredTraits.sumOf { it.weight * 100.0 }
        return (requiredMax + preferredMax).coerceAtLeast(100.0)
    }
}
