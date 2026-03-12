package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.model.breeding.*
import com.example.hatchtracker.model.genetics.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BreederActionInterpreter @Inject constructor() {

    /**
     * Converts a raw GeneticInsightReport into a formal AdvisoryContract.
     */
    fun interpret(
        report: GeneticInsightReport,
        scenario: BreedingScenarioProfile?
    ): GeneticInsightAdvisoryContract {
        val summaryCodes = mutableListOf<String>()
        val actions = mutableListOf<BreederActionSuggestion>()

        // 1. Determine Data Strength (Composer Role)
        val strengthTier = evaluateDataStrength(report, scenario)
        val whyUnavailable = if (strengthTier != GeneticDataStrengthTier.FULL) {
            determinePrimaryUnavailabilityReason(report, scenario)
        } else GeneticInsightAdvisoryContract.UNAVAILABLE_NONE

        // 2. Map Insight States to Summary Codes (Whitelist Enforced)
        mapInsightsToCodes(report, summaryCodes)

        // 3. Derive Actions with Prioritization
        deriveActionSuggestions(report, scenario, actions)

        // 4. Resolve Consistency (No contradiction guard)
        val filteredActions = resolveConsistency(actions, report)

        // 5. Select Top Signals (Strict presentation-level logic)
        val topAction = filteredActions.firstOrNull()
        val topWarning = report.insights
            .filter { it.riskLevel == VariabilityRiskLevel.EXTREME || it.riskLevel == VariabilityRiskLevel.HIGH }
            .maxByOrNull { it.riskLevel }
            ?.type?.name ?: GeneticInsightAdvisoryContract.WARNING_NONE

        // 6. Fallback if empty
        if (summaryCodes.isEmpty() && strengthTier == GeneticDataStrengthTier.COMPACT) {
            summaryCodes.add(GeneticAdvisoryCodes.LIMITED_INTELLIGENCE)
        } else if (summaryCodes.isEmpty()) {
            summaryCodes.add(GeneticAdvisoryCodes.NO_STRONG_SIGNAL)
        }

        return GeneticInsightAdvisoryContract(
            insightSummaryCodes = summaryCodes.distinct().take(5),
            topWarningCode = topWarning,
            topActionCategory = topAction?.category?.name ?: GeneticInsightAdvisoryContract.ACTION_NONE,
            confidenceBand = report.globalConfidence,
            dataStrengthTier = strengthTier,
            whyUnavailableCode = whyUnavailable
        )
    }

    private fun evaluateDataStrength(report: GeneticInsightReport, scenario: BreedingScenarioProfile?): GeneticDataStrengthTier {
        val hasPedigree = report.generationLabel != "Uncertain" && report.generationLabel != "Limited Pedigree Information"
        val hasComposition = report.breedComposition.isNotEmpty()
        val hasConfidence = report.globalConfidence != InsightConfidence.LOW

        return when {
            hasPedigree && hasComposition && hasConfidence -> GeneticDataStrengthTier.FULL
            hasComposition && hasConfidence -> GeneticDataStrengthTier.PARTIAL
            else -> GeneticDataStrengthTier.COMPACT
        }
    }

    private fun determinePrimaryUnavailabilityReason(report: GeneticInsightReport, scenario: BreedingScenarioProfile?): String {
        return when {
            report.generationLabel == "Limited Pedigree Information" -> GeneticAdvisoryCodes.MISSING_PEDIGREE
            report.breedComposition.isEmpty() -> GeneticAdvisoryCodes.MISSING_BREED_COMPOSITION
            scenario == null -> GeneticAdvisoryCodes.NO_ACTIVE_SCENARIO
            report.globalConfidence == InsightConfidence.LOW -> GeneticAdvisoryCodes.LOW_CONFIDENCE_METADATA
            else -> GeneticInsightAdvisoryContract.UNAVAILABLE_NONE
        }
    }

    private fun mapInsightsToCodes(report: GeneticInsightReport, codes: MutableList<String>) {
        report.insights.forEach { insight ->
            val code = when (insight.type) {
                GeneticInsightType.F2_SEGREGATION -> GeneticAdvisoryCodes.HIGH_VARIABILITY_F2
                GeneticInsightType.INBREEDING_WARNING -> GeneticAdvisoryCodes.PREMATURE_FIXATION
                GeneticInsightType.STABILIZATION_PROGRESS -> if (report.stabilizationForecast?.isStagnating == true) GeneticAdvisoryCodes.STABILIZATION_LAG else null
                else -> null
            }
            code?.let { codes.add(it) }
        }
        if (report.heterosisEstimate?.presence == true) codes.add(GeneticAdvisoryCodes.HYBRID_VIGOR_PRESENT)
    }

    private fun deriveActionSuggestions(
        report: GeneticInsightReport,
        scenario: BreedingScenarioProfile?,
        actions: MutableList<BreederActionSuggestion>
    ) {
        // High Variability (F2)
        if (report.generationLabel == "F2") {
            actions.add(
                BreederActionSuggestion(
                    id = "F2_SELECTION",
                    category = BreederActionCategory.SELECTION,
                    title = UiText.DynamicString("Selection Window Open"),
                    suggestion = UiText.DynamicString("This is a peak variability generation. Select birds that strictly meet your trait goals."),
                    urgencyScore = 80,
                    confidence = report.globalConfidence
                )
            )
        }

        // Stabilization Lag
        if (report.stabilizationForecast?.isStagnating == true) {
            actions.add(
                BreederActionSuggestion(
                    id = "STABILIZATION_BOOST",
                    category = BreederActionCategory.STRATEGY,
                    title = UiText.DynamicString("Increase Selection Pressure"),
                    suggestion = UiText.DynamicString("Stabilization is lagging. Apply stricter selection criteria to accelerate progress."),
                    urgencyScore = 60,
                    confidence = report.globalConfidence
                )
            )
        }

        // Premature Fixation
        if (report.insights.any { it.type == GeneticInsightType.INBREEDING_WARNING }) {
            actions.add(
                BreederActionSuggestion(
                    id = "REDUCE_FIXATION",
                    category = BreederActionCategory.DIVERSITY,
                    title = UiText.DynamicString("Introduce New Blood"),
                    suggestion = UiText.DynamicString("Risk of bottlenecking detected. Consider outcrossing to an unrelated line."),
                    urgencyScore = 95,
                    confidence = report.globalConfidence,
                    associatedRisk = VariabilityRiskLevel.EXTREME
                )
            )
        }

        // Sort by priority logic: Urgency DESC, Confidence DESC
        actions.sortByDescending { it.urgencyScore }
    }

    private fun resolveConsistency(actions: List<BreederActionSuggestion>, report: GeneticInsightReport): List<BreederActionSuggestion> {
        // Dedup and conflict resolution
        val seenCategories = mutableSetOf<BreederActionCategory>()
        return actions.filter { 
            if (seenCategories.contains(it.category)) false
            else {
                seenCategories.add(it.category)
                true
            }
        }
    }
}
