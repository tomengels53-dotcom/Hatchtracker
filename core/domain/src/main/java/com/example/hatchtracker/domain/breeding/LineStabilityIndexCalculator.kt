package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.TraitTarget
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.genetics.Certainty
import com.example.hatchtracker.model.genetics.GenotypeDistribution
import kotlin.math.ln

/**
 * Calculator for Genetic Line Stability Index (GLSI).
 */
object LineStabilityIndexCalculator {

    private const val WEIGHT_FIXATION = 0.35
    private const val WEIGHT_VARIANCE = 0.25
    private const val WEIGHT_RISK = 0.20
    private const val WEIGHT_DIVERSITY = 0.10
    private const val WEIGHT_CONFIDENCE = 0.10

    fun compute(
        species: com.example.hatchtracker.model.Species,
        generationIndex: Int,
        prediction: BreedingPredictionResult,
        goal: com.example.hatchtracker.data.models.TraitTarget?,
        risk: BreedingRiskResult,
        diversityMeta: DiversityMeta,
        previous: LineStabilitySnapshot? = null,
        mustHaveIds: Set<String> = emptySet()
    ): LineStabilitySnapshot {
        val components = StabilityComponents(
            fixation = calculateFixationScore(prediction, mustHaveIds),
            variance = calculateVarianceScore(prediction),
            risk = calculateRiskScore(risk),
            diversity = calculateDiversityScore(diversityMeta),
            confidence = calculateConfidenceScore(prediction)
        )

        val glsiScore = (100 * (
            WEIGHT_FIXATION * components.fixation +
            WEIGHT_VARIANCE * components.variance +
            WEIGHT_RISK * components.risk +
            WEIGHT_DIVERSITY * components.diversity +
            WEIGHT_CONFIDENCE * components.confidence
        )).toInt().coerceIn(0, 100)

        val drivers = generateDrivers(components, previous)
        val banner = checkBannerTriggers(glsiScore, risk, previous)

        val isEstablished = components.fixation >= 0.90 && 
            components.variance <= 0.20 && 
            prediction.phenotypeResult.probabilities.any { it.probability >= 0.95 }

        return LineStabilitySnapshot(
            glsiScore = glsiScore,
            components = components,
            drivers = drivers,
            banner = banner,
            isEstablished = isEstablished,
            fixationProgress = components.fixation,
            effectivePopulation = risk.effectivePopulation
        )
    }

    private fun calculateFixationScore(prediction: BreedingPredictionResult, mustHaveIds: Set<String>): Double {
        val distributions = prediction.offspringDistributions
        if (distributions.isEmpty()) return 1.0

        val mustHaveFixation = distributions.filterKeys { it in mustHaveIds }
            .map { (_, dist) -> calculateLocusHomozygosity(dist) }

        val otherFixation = distributions.filterKeys { it !in mustHaveIds }
            .map { (_, dist) -> calculateLocusHomozygosity(dist) }

        val mScore = if (mustHaveFixation.isNotEmpty()) mustHaveFixation.average() else 1.0
        val oScore = if (otherFixation.isNotEmpty()) otherFixation.average() else 1.0

        // must-have loci contribute 70%, others 30%
        return (mScore * 0.7 + oScore * 0.3).coerceIn(0.0, 1.0)
    }

    private fun calculateLocusHomozygosity(dist: com.example.hatchtracker.model.genetics.GenotypeDistribution): Double {
        return dist.outcomes.entries.sumOf { (genotype, prob) ->
            val isHomozygous = genotype.alleles.size == 2 && genotype.alleles[0] == genotype.alleles[1]
            if (isHomozygous) prob else 0.0
        }
    }

    private fun calculateVarianceScore(prediction: BreedingPredictionResult): Double {
        val phenotypes = prediction.phenotypeResult.probabilities
        if (phenotypes.isEmpty()) return 1.0

        val totalEntropy = phenotypes.sumOf { it.probability } / phenotypes.size
        return totalEntropy.coerceIn(0.0, 1.0)
    }

    private fun calculateRiskScore(risk: BreedingRiskResult): Double {
        return (1.0 - (risk.bottleneckRiskScore.toDouble() / 100.0)).coerceIn(0.0, 1.0)
    }

    private fun calculateDiversityScore(meta: DiversityMeta): Double {
        if (meta.totalBirds <= 1) return 1.0
        val base = meta.uniqueSires.toDouble() / meta.totalBirds.toDouble()
        val penalty = (meta.repeatedSireCount.toDouble() * 0.2).coerceIn(0.0, 0.5)
        return (base - penalty).coerceIn(0.0, 1.0)
    }

    private fun calculateConfidenceScore(prediction: BreedingPredictionResult): Double {
        val certaintyValues = prediction.sireGenotypes.values + prediction.damGenotypes.values
        if (certaintyValues.isEmpty()) return 0.3

        val avg = certaintyValues.map {
            when (it.certainty) {
                Certainty.CONFIRMED -> 1.0
                Certainty.ASSUMED -> 0.6
                Certainty.UNKNOWN -> 0.3
            }
        }.average()

        return avg.coerceIn(0.0, 1.0)
    }

    private fun generateDrivers(current: StabilityComponents, previous: LineStabilitySnapshot?): List<StabilityDriver> {
        val drivers = mutableListOf<StabilityDriver>()
        if (previous == null) {
            drivers.add(StabilityDriver("Line Configuration", "Initial genetic baseline.", DriverImpact.POSITIVE, WEIGHT_FIXATION, 0.0))
            return drivers
        }

        val prev = previous.components
        addDriver(drivers, "Genetic Fixation", current.fixation - prev.fixation, WEIGHT_FIXATION, "Trait stability changed.")
        addDriver(drivers, "Outcome Consistency", current.variance - prev.variance, WEIGHT_VARIANCE, "Predictability modified.")
        addDriver(drivers, "Inbreeding Exposure", current.risk - prev.risk, WEIGHT_RISK, "Bottleneck risk level shifted.")
        addDriver(drivers, "Sire Diversity", current.diversity - prev.diversity, WEIGHT_DIVERSITY, "Unique ancestor representation changed.")
        addDriver(drivers, "Data Confidence", current.confidence - prev.confidence, WEIGHT_CONFIDENCE, "Certainty of underlying genotypes.")

        return drivers.sortedByDescending { Math.abs(it.delta * it.weight) }.take(5)
    }

    private fun addDriver(list: MutableList<StabilityDriver>, name: String, delta: Double, weight: Double, desc: String) {
        if (Math.abs(delta) < 0.01) return
        val impact = when {
            delta > 0 -> DriverImpact.POSITIVE
            name.contains("Risk", ignoreCase = true) -> DriverImpact.WARNING
            else -> DriverImpact.NEGATIVE
        }
        list.add(StabilityDriver(name, desc, impact, weight, delta))
    }

    private fun checkBannerTriggers(
        currentScore: Int,
        risk: BreedingRiskResult,
        previous: LineStabilitySnapshot?
    ): StabilityWarningBanner? {
        if (previous == null) return null
        val deltaGLSI = currentScore - previous.glsiScore
        val prevRiskScore = (1.0 - previous.components.risk) * 100
        val deltaBottleneck = risk.bottleneckRiskScore - prevRiskScore.toInt()

        val trapTriggered = deltaGLSI >= 5 && 
            (deltaBottleneck >= 15) && 
            (risk.bottleneckRiskScore >= 45 || risk.riskLevel == RiskLevel.HIGH_RISK)

        if (trapTriggered) {
            return StabilityWarningBanner(
                title = "Stability improved, but risk is rising fast",
                body = "Inbreeding bottleneck rose from ${prevRiskScore.toInt()} to ${risk.bottleneckRiskScore}.",
                ctaPrimary = "Show safer alternative",
                ctaSecondary = "Adjust constraints"
            )
        }
        return null
    }
}

