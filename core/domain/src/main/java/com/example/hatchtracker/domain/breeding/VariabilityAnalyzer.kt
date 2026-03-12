package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.genetics.*
import com.example.hatchtracker.domain.breeding.BreedingPredictionResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VariabilityAnalyzer @Inject constructor(
    private val confidenceEvaluator: InsightConfidenceEvaluator
) {
    fun analyze(
        generationLabel: String,
        breedComposition: List<BreedContribution>,
        prediction: BreedingPredictionResult,
        scenario: BreedingScenarioProfile? = null
    ): VariabilityProfile {
        var score = 0.8 // Default uniformity
        val factors = mutableListOf<String>()
        val inputs = mutableListOf("generationLabel", "prediction")
        val assumptions = mutableListOf<String>()
        
        when {
            generationLabel == "F1" -> {
                score = 0.95
                factors.add("F1 Hybrid Uniformity: Consistent first-cross performance.")
            }
            generationLabel == "F2" -> {
                score = 0.2
                factors.add("Peak Segregation (F2): Maximum genetic recombination.")
            }
            generationLabel.startsWith("BC") -> {
                score = 0.65
                factors.add("Backcross Recovery: Returning to recurrent parent traits.")
            }
            generationLabel.startsWith("F") -> {
                val num = generationLabel.substring(1).toIntOrNull() ?: 3
                if (num >= 6) {
                    score = 0.85
                    factors.add("Advanced Fixation: Approaching breed stability.")
                } else {
                    score = 0.4 + (num * 0.05)
                    factors.add("Progressive Selection: Reducing variability toward fixation.")
                }
            }
        }

        if (breedComposition.size > 2) {
            score *= 0.85
            factors.add("Composite Complexity: Multiple foundational lines increase outcome diversity.")
            inputs.add("breedComposition")
        }

        val riskLevel = when {
            score > 0.8 -> VariabilityRiskLevel.LOW
            score > 0.5 -> VariabilityRiskLevel.MODERATE
            score > 0.25 -> VariabilityRiskLevel.HIGH
            else -> VariabilityRiskLevel.EXTREME
        }

        // Scenario Integration: If user TOLERATES variability (e.g. for selection), adjust interpretation
        val evidence = confidenceEvaluator.evaluate(
            inputs = inputs,
            assumptions = assumptions,
            derivationPath = "generation-variability-model",
            potentialMissing = listOf("traitHeritability")
        )

        return VariabilityProfile(
            uniformityScore = score,
            riskLevel = riskLevel,
            primaryFactors = factors,
            evidence = evidence
        )
    }
}
