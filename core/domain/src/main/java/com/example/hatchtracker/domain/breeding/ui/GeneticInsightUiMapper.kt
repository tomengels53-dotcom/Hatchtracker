package com.example.hatchtracker.domain.breeding.ui

import com.example.hatchtracker.model.breeding.*
import com.example.hatchtracker.model.genetics.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneticInsightUiMapper @Inject constructor() {

    /**
     * Maps the advisory contract to a UI-ready model.
     * Enforces presentation-level caps (max 1 warning, 2 actions, 2 indicators).
     */
    fun map(
        contract: GeneticInsightAdvisoryContract,
        report: GeneticInsightReport
    ): GeneticInsightUiModel {
        // Enforce Visual Restraint
        val topActions = getTopActions(contract)
        
        // Controlled Vocabulary for Stability & Diversity
        val stabilityLabel = mapStabilityLabel(report.stabilizationForecast?.progressPercentage ?: 0)
        val diversityLabel = report.diversityIndicator?.interpretation ?: "Unknown"

        return GeneticInsightUiModel(
            title = "Breeding Intelligence",
            summary = generateSummary(contract),
            stabilityScore = report.stabilizationForecast?.progressPercentage ?: 0,
            stabilityLabel = stabilityLabel,
            diversityLabel = diversityLabel,
            topWarning = if (contract.topWarningCode != GeneticInsightAdvisoryContract.WARNING_NONE) 
                mapWarningLabel(contract.topWarningCode) else null,
            primaryActions = topActions,
            confidence = contract.confidenceBand,
            whyThisMatters = generateExplanationBasis(contract, report),
            isLoading = false
        )
    }

    private fun getTopActions(contract: GeneticInsightAdvisoryContract): List<BreederActionSuggestion> {
        // In a real app, this would pull full Action objects from a registry based on the category
        return emptyList() // Placeholder: Detailed action mapping happens in BreederActionUiMapper
    }

    private fun mapStabilityLabel(score: Int): String = when {
        score >= 75 -> "Stable Line"
        score >= 50 -> "Stabilizing"
        score >= 25 -> "High Variability"
        else -> "Early Hybrid"
    }

    private fun mapWarningLabel(code: String): String = when (code) {
        GeneticAdvisoryCodes.INBREEDING_RISK -> "High Inbreeding Risk"
        GeneticAdvisoryCodes.TRAIT_INSTABILITY -> "Trait Segregation Danger"
        else -> code.replace("_", " ")
    }

    private fun generateSummary(contract: GeneticInsightAdvisoryContract): String {
        if (contract.dataStrengthTier == GeneticDataStrengthTier.COMPACT) {
            return "Limited breeding intelligence available for this pairing."
        }
        return "Analysis indicates ${contract.insightSummaryCodes.firstOrNull()?.replace("_", " ")?.lowercase() ?: "stable genetics"}."
    }

    private fun generateExplanationBasis(contract: GeneticInsightAdvisoryContract, report: GeneticInsightReport): String? {
        if (contract.whyUnavailableCode != GeneticInsightAdvisoryContract.UNAVAILABLE_NONE) {
            return when (contract.whyUnavailableCode) {
                GeneticAdvisoryCodes.MISSING_PEDIGREE -> "Full pedigree history is missing for one or both parents."
                GeneticAdvisoryCodes.MISSING_BREED_COMPOSITION -> "Breed composition metadata is incomplete."
                else -> "Metadata is insufficient for high-confidence advice."
            }
        }
        return "Based on ${report.generationLabel} ancestry and current trait targets."
    }
}
