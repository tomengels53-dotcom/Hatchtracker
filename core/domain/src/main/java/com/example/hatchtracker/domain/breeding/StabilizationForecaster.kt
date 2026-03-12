package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.genetics.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StabilizationForecaster @Inject constructor(
    private val confidenceEvaluator: InsightConfidenceEvaluator
) {
    /**
     * Forecasts stabilization timeline with population size awareness.
     */
    fun forecast(
        generationLabel: String,
        variability: VariabilityProfile,
        populationSize: Int = 10,
        selectedBreeders: Int = 2
    ): StabilizationForecast {
        val currentGen = generationLabel.filter { it.isDigit() }.toIntOrNull() ?: 1
        val baseline = 7 
        
        // 1. Progress Calculation
        val baseProgress = when (generationLabel) {
            "F1" -> 0.05
            "F2" -> 0.10
            "F3" -> 0.25
            "F4" -> 0.50
            "F5" -> 0.75
            "F6" -> 0.90
            else -> 0.95
        }

        // 2. Population & Selection Hardening
        val selectionIntensity = selectedBreeders.toDouble() / populationSize
        val populationModifier = if (populationSize < 8) 0.7 else 1.0
        val selectionModifier = if (selectionIntensity < 0.2) 1.2 else 1.0 // High pressure boost
        
        val finalProgress = (baseProgress * populationModifier * selectionModifier).coerceIn(0.0, 1.0)

        // 3. Risk Signal (Structural semantics)
        val inbreedingRisk = when {
            populationSize < 8 -> "CRITICAL_BOTTLENECK"
            currentGen >= 5 && selectedBreeders < 4 -> "HIGH_SELECTION_PRESSURE"
            else -> "STABLE"
        }

        return StabilizationForecast(
            progressPercentage = (finalProgress * 100).toInt(),
            isStagnating = false,
            evidence = InsightEvidence(
                confidence = InsightConfidence.MODERATE,
                directEvidence = listOf("generationLabel", "variabilityProfile", "populationSize", "selectionIntensity"),
                derivationPath = "population-weighted-drift-model"
            )
        )
    }
}
