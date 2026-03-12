package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.genetics.InsightConfidence
import com.example.hatchtracker.model.genetics.InsightEvidence
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightConfidenceEvaluator @Inject constructor() {
    
    fun evaluate(
        inputs: List<String>,
        assumptions: List<String>,
        derivationPath: String,
        potentialMissing: List<String> = emptyList()
    ): InsightEvidence {
        val missing = potentialMissing.filter { it !in inputs }
        val reasons = mutableListOf<String>()
        
        var confidence = when {
            missing.size >= 2 || assumptions.size >= 3 -> InsightConfidence.LOW
            missing.isNotEmpty() || assumptions.isNotEmpty() -> InsightConfidence.MODERATE
            inputs.size >= 3 -> InsightConfidence.HIGH
            else -> InsightConfidence.MODERATE
        }

        if (missing.isNotEmpty()) {
            reasons.add("Limited by missing: ${missing.joinToString(", ")}")
        }
        if (assumptions.isNotEmpty()) {
            reasons.add("Relies on assumptions: ${assumptions.joinToString(", ")}")
        }
        if (confidence == InsightConfidence.HIGH) {
            reasons.add("Verified via complete lineage and trait metadata.")
        }

        return InsightEvidence(
            confidence = confidence,
            directEvidence = inputs,
            derivationPath = derivationPath,
            confidenceReasons = reasons,
            missingInputs = missing
        )
    }
}
