package com.example.hatchtracker.model.breeding

import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.model.genetics.InsightConfidence
import com.example.hatchtracker.model.genetics.VariabilityRiskLevel

/**
 * Prioritized action suggestion for the breeder based on genetic analysis.
 */
data class BreederActionSuggestion(
    val id: String,
    val category: BreederActionCategory,
    val title: UiText,
    val suggestion: UiText,
    val urgencyScore: Int, // 0 to 100
    val confidence: InsightConfidence,
    val associatedRisk: VariabilityRiskLevel = VariabilityRiskLevel.LOW
)

enum class BreederActionCategory {
    SELECTION,   // Guidance on which birds to pick/cull
    DIVERSITY,   // Guidance on maintaining flock health
    STRATEGY,    // Long-term breeding roadmap guidance
    MANAGEMENT,  // Immediate flock care/separation guidance
    NONE         // No actionable advice
}

/**
 * Stable UI state for the Breeder Intelligence Card.
 */
data class GeneticInsightUiModel(
    val title: String,
    val summary: String,
    val stabilityScore: Int,
    val stabilityLabel: String,
    val diversityLabel: String,
    val topWarning: String? = null,
    val primaryActions: List<BreederActionSuggestion> = emptyList(),
    val confidence: InsightConfidence = InsightConfidence.MODERATE,
    val whyThisMatters: String? = null,
    val confidenceNote: String? = null,
    val isLoading: Boolean = false
)
