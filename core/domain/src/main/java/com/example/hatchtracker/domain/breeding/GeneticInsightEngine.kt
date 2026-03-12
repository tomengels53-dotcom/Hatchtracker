package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.genetics.*
import com.example.hatchtracker.domain.breeding.BreedingPredictionResult
import com.example.hatchtracker.model.UiText
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

@Singleton
class GeneticInsightEngine @Inject constructor(
    private val variabilityAnalyzer: VariabilityAnalyzer,
    private val heterosisEstimator: HeterosisEstimator,
    private val stabilizationForecaster: StabilizationForecaster,
    private val breedCompositionInterpreter: BreedCompositionInterpreter,
    private val confidenceEvaluator: InsightConfidenceEvaluator
) {
    private val extraContributors = mutableListOf<InsightContributor>()

    fun registerContributor(contributor: InsightContributor) {
        if (extraContributors.none { it.id == contributor.id }) {
            extraContributors.add(contributor)
        }
    }

    /**
     * Hardened analysis entry point.
     * 1. Canonicalizes request inputs.
     * 2. Orchestrates isolated contributors with timeout guards.
     * 3. Enforces diversity-stability consistency rules.
     */
    suspend fun analyzePairing(
        species: Species,
        sire: Bird,
        dam: Bird,
        prediction: BreedingPredictionResult,
        scenario: BreedingScenarioProfile? = null,
        catalogVersion: Int = 1
    ): GeneticInsightReport {
        val startTime = System.currentTimeMillis()
        val executedContributors = mutableListOf<String>()
        val fallbacks = mutableListOf<String>()

        // 1. Request Normalization & Canonicalization
        val normalizedSire = sire 
        val normalizedDam = dam
        val normalizedScenario = scenario?.copy(
            goalType = scenario.goalType.uppercase(),
            breedingMode = scenario.breedingMode.uppercase()
        )

        // 2. Derive Label with Ancestry Caution (Hardened Fallback)
        val genLabel = deriveGenerationLabel(normalizedSire, normalizedDam)
        
        // 3. Derive Composition (Hardened & Canonical)
        val breedComp = breedCompositionInterpreter.deriveComposition(normalizedSire, normalizedSire, normalizedDam)
        
        // 4. Core Analytics
        val variability = variabilityAnalyzer.analyze(genLabel, breedComp, prediction, normalizedScenario)
        val heterosis = heterosisEstimator.estimate(normalizedSire, normalizedDam, breedComp)
        
        // 5. Interaction Rules: Diversity vs Stability
        val diversity = calculateDiversity(breedComp)
        var stabilization = stabilizationForecaster.forecast(genLabel, variability)

        val finalInsights = mutableListOf<GeneticInsight>()
        val summaryTags = mutableListOf<String>()

        // Interaction Rule: Premature fixation (Bottleneck)
        if (diversity.entropyScore < 20.0 && (genLabel == "F1" || genLabel == "F2")) {
            summaryTags.add("PREMATURE_FIXATION_WARNING")
            finalInsights.add(
                GeneticInsight(
                    type = GeneticInsightType.INBREEDING_WARNING,
                    title = UiText.DynamicString("Premature Fixation Risk"),
                    plainExplanation = UiText.DynamicString("Diversity is exceptionally low for this early generation."),
                    scenarioExplanation = UiText.DynamicString("Risk of bottlenecking before desired traits are stabilized."),
                    actionGuidance = UiText.DynamicString("Consider introducing unrelated birds to restore genetic diversity."),
                    riskLevel = VariabilityRiskLevel.EXTREME,
                    evidence = diversity.evidence
                )
            )
        }

        // Interaction Rule: High diversity vs Advanced generation (Stabilization Lag)
        if (diversity.entropyScore > 60.0 && genLabel.startsWith("F") && (genLabel.filter { it.isDigit() }.toIntOrNull() ?: 0) >= 4) {
             summaryTags.add("STABILIZATION_LAG")
             stabilization = stabilization.copy(
                 progressPercentage = (stabilization.progressPercentage * 0.8).toInt(),
                 isStagnating = true
             )
             finalInsights.add(
                 GeneticInsight(
                     type = GeneticInsightType.STABILIZATION_PROGRESS,
                     title = UiText.DynamicString("Stabilization Lag"),
                     plainExplanation = UiText.DynamicString("Multiple redundant breed backgrounds are slowing fixation."),
                     actionGuidance = UiText.DynamicString("Stronger selection for specific traits may accelerate stabilization."),
                     riskLevel = VariabilityRiskLevel.MODERATE,
                     evidence = diversity.evidence
                 )
             )
        }

        // F2 Segregation
        if (genLabel == "F2") {
            summaryTags.add("HIGH_VARIABILITY_F2")
            finalInsights.add(createF2Insight(variability, normalizedScenario))
        }

        if (heterosis?.presence == true) {
            summaryTags.add("HYBRID_VIGOR_PRESENT")
        }

        val assumptions = mutableListOf<String>()
        val unavailable = mutableListOf<GeneticInsightType>()

        // 6. Initial Report (Structured Semantics)
        var report = GeneticInsightReport(
            reportVersion = 2,
            catalogVersion = catalogVersion,
            generationLabel = genLabel,
            breedComposition = breedComp,
            variabilityProfile = variability,
            heterosisEstimate = heterosis,
            stabilizationForecast = stabilization,
            diversityIndicator = diversity,
            selectionPressureImpact = null,
            insights = finalInsights,
            summaryTags = summaryTags,
            assumptionsUsed = assumptions,
            fallbacksUsed = fallbacks,
            unavailableInsights = unavailable,
            globalConfidence = deriveGlobalConfidence(listOfNotNull(variability.evidence, heterosis?.evidence, stabilization.evidence))
        )

        // 7. Isolated Contributor Execution (Timeout Guarded)
        extraContributors
            .sortedByDescending { it.priority }
            .forEach { contributor ->
                try {
                    val result = withTimeoutOrNull(50) {
                        if (contributor.supports(normalizedScenario, report)) {
                            executedContributors.add(contributor.id)
                            contributor.contribute(normalizedScenario ?: BreedingScenarioProfile("GENERAL_IMPROVEMENT", "OUT_CROSS"), report)
                        } else null
                    }
                    if (result != null) {
                        finalInsights.addAll(result)
                    } else if (contributor.supports(normalizedScenario, report)) {
                        println("GeneticInsightEngine: Contributor '${contributor.id}' timed out (>50ms).")
                        unavailable.add(GeneticInsightType.HYBRID_INSTABILITY)
                    }
                } catch (e: Exception) {
                    println("GeneticInsightEngine: Contributor '${contributor.id}' crashed. Isolating.")
                    unavailable.add(GeneticInsightType.HYBRID_INSTABILITY)
                    report = report.copy(globalConfidence = InsightConfidence.LOW)
                }
            }

        val endTime = System.currentTimeMillis()
        val trace = GeneticInsightTrace(
            contributorsExecuted = executedContributors,
            executionTimeMs = endTime - startTime,
            fallbacksTriggered = fallbacks
        )

        return report.copy(
            insights = finalInsights, 
            unavailableInsights = unavailable,
            trace = trace,
            summaryTags = summaryTags.distinct()
        )
    }

    private fun createF2Insight(variability: VariabilityProfile, scenario: BreedingScenarioProfile?): GeneticInsight {
        return GeneticInsight(
            type = GeneticInsightType.F2_SEGREGATION,
            title = UiText.DynamicString("Peak Variability (F2)"),
            plainExplanation = UiText.DynamicString("Maximum trait segregation in second generation."),
            scenarioExplanation = UiText.DynamicString(
                if (scenario?.goalType == "TRAIT_SELECTION") "Supports selection palette." else "Slows stabilization progress."
            ),
            riskLevel = VariabilityRiskLevel.HIGH,
            evidence = variability.evidence,
            alignmentWithGoal = if (scenario?.goalType == "TRAIT_SELECTION") 1.0 else 0.2
        )
    }

    /**
     * Derives label with extreme caution.
     * Prefers "Limited Pedigree Information" over false precision for partial ancestry.
     */
    fun deriveGenerationLabel(sire: Bird, dam: Bird): String {
        val sLabel = sire.generationLabel
        val dLabel = dam.generationLabel
        
        if (sLabel == "Uncertain" || dLabel == "Uncertain") return "Uncertain"

        if (sire.breedId == dam.breedId && (sLabel == null || sLabel == "Purebred") && (dLabel == null || dLabel == "Purebred")) {
            return "Purebred"
        }

        if (sire.breedId != dam.breedId && (sLabel == null || sLabel == "Purebred") && (dLabel == null || dLabel == "Purebred")) {
            return "F1"
        }

        // 4. Incomplete Pedigree Warning (Softer wording)
        if (sLabel == null || dLabel == null) {
            return "Limited Pedigree Information"
        }

        if (sLabel == dLabel && sLabel.startsWith("F")) {
            val num = sLabel.substring(1).toIntOrNull() ?: return "Uncertain"
            return if (num >= 7) "Synthetic Line" else "F${num + 1}"
        }

        if (sLabel.startsWith("F") && dLabel.startsWith("F")) {
             val sNum = sLabel.substring(1).toIntOrNull() ?: 0
             val dNum = dLabel.substring(1).toIntOrNull() ?: 0
             return "F${minOf(sNum, dNum) + 1} (Mixed)"
        }

        return "Uncertain"
    }

    private fun calculateDiversity(composition: List<BreedContribution>): GeneticDiversityIndicator {
        // Clamp Rule: if only one breed is present, diversity is exactly 0.
        if (composition.size <= 1) {
            return GeneticDiversityIndicator(
                entropyScore = 0.0,
                interpretation = "Highly Fixed",
                evidence = InsightEvidence(InsightConfidence.HIGH, listOf("single-breed-monoculture"))
            )
        }
        
        val entropy = composition.sumOf { 
            val p = it.percentage
            if (p > 0) -(p * ln(p)) else 0.0
        }
        
        val maxEntropy = ln(4.0)
        val normalized = (entropy / maxEntropy).coerceIn(0.0, 1.0) * 100.0
        
        val interpretation = when {
            normalized >= 75 -> "Highly Diverse"
            normalized >= 40 -> "Moderate Diversity"
            normalized >= 20 -> "Low Diversity"
            else -> "Highly Fixed"
        }

        return GeneticDiversityIndicator(
            entropyScore = normalized,
            interpretation = interpretation,
            evidence = InsightEvidence(InsightConfidence.MODERATE, listOf("breedComposition"))
        )
    }

    private fun deriveGlobalConfidence(evidences: List<InsightEvidence>): InsightConfidence {
        if (evidences.any { it.confidence == InsightConfidence.LOW }) return InsightConfidence.LOW
        if (evidences.all { it.confidence == InsightConfidence.HIGH }) return InsightConfidence.HIGH
        return InsightConfidence.MODERATE
    }
}
