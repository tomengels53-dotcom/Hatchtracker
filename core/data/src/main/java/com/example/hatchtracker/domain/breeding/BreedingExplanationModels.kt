package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.domain.prediction.ConfidenceLevel

data class BreedingExplanation(
    val pairId: String = "", // "maleId_femaleId"
    val summary: String = "", // "This looks like a strong pairing for egg color!"
    val details: List<ExplanationDetail> = emptyList(),
    val warnings: List<ExplanationWarning> = emptyList(),
    val improvementTips: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class ExplanationDetail(
    val traitName: String, // e.g., "Egg Color"
    val prediction: String, // "Mostly Blue"
    val probabilityLabel: String, // "High Chance (85%)"
    val isInferred: Boolean, // True if based on non-fixed traits
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.LOW// Used to style the text
)

data class ExplanationWarning(
    val message: String,
    val severity: WarningSeverity, // INFO, CAUTION, CRITICAL
    val type: WarningType // INBREEDING, LOW_CONFIDENCE, CONFLICT
)

enum class WarningSeverity {
    INFO, CAUTION, CRITICAL
}

enum class WarningType {
    INBREEDING, LOW_CONFIDENCE, CONFLICT, GENERAL
}
