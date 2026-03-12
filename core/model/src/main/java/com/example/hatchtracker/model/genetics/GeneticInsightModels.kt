package com.example.hatchtracker.model.genetics

import com.example.hatchtracker.model.UiText

/**
 * High-level breed classification to drive variability logic.
 */
enum class BreedType {
    HERITAGE,
    HYBRID,
    INDUSTRIAL,
    LANDRACE
}

/**
 * Categorization of genetic variability risk.
 */
enum class VariabilityRiskLevel {
    LOW,
    MODERATE,
    HIGH,
    EXTREME
}

/**
 * Value object for breed contribution to a bird's genetics.
 */
data class BreedContribution(
    val breedId: String,
    val percentage: Double
)

/**
 * Indicates the reliability of an engine output.
 */
enum class InsightConfidence {
    LOW,      // Significant assumptions or missing metadata
    MODERATE, // Built on species defaults or incomplete but representative data
    HIGH      // Built on specific trait metadata and full pedigree
}

/**
 * Describes the basis for an insight.
 */
data class InsightEvidence(
    val confidence: InsightConfidence,
    val directEvidence: List<String> = emptyList(),
    val derivationPath: String = "",
    val confidenceReasons: List<String> = emptyList(),
    val missingInputs: List<String> = emptyList()
)

/**
 * Extension contract for modular genetics contributors.
 */
interface InsightContributor {
    val id: String
    val priority: Int get() = 100
    val requiredIds: List<String> get() = emptyList()
    
    fun supports(scenario: BreedingScenarioProfile?, report: GeneticInsightReport): Boolean
    
    fun contribute(
        scenario: BreedingScenarioProfile, 
        report: GeneticInsightReport
    ): List<GeneticInsight>
}

/**
 * Goal-aware context for genetic analysis.
 */
data class BreedingScenarioProfile(
    val goalType: String, // e.g. "STABILIZATION", "TRAIT_SELECTION", "PRODUCTION"
    val breedingMode: String, // e.g. "BACKCROSS", "OUT_CROSS", "LINE_BREEDING"
    val variabilityTolerance: Double = 0.5,
    val preferredTimeHorizonGens: Int = 5
)

/**
 * Structured insight about a breeding pairing or population.
 */
data class GeneticInsight(
    val type: GeneticInsightType,
    val title: UiText,
    val plainExplanation: UiText,
    val scenarioExplanation: UiText? = null,
    val advancedExplanation: UiText? = null,
    val actionGuidance: UiText? = null,
    val riskLevel: VariabilityRiskLevel = VariabilityRiskLevel.LOW,
    val evidence: InsightEvidence? = null,
    val alignmentWithGoal: Double = 0.5
)

enum class GeneticInsightType {
    F1_UNIFORMITY,
    F2_SEGREGATION,
    HETEROSIS_REDUCTION,
    STABILIZATION_PROGRESS,
    INBREEDING_WARNING,
    HYBRID_INSTABILITY,
    BACKCROSS_TREND,
    SELECTION_OPPORTUNITY
}

/**
 * Summary of a crossbreeding population analysis.
 * 
 * DESIGN RULE: Keep this structure lightweight (< 10KB serialized) for 
 * efficient Hatchy prompts and UI responsiveness.
 */
data class GeneticInsightReport(
    val reportVersion: Int = 1,
    val catalogVersion: Int = 1,
    val generationLabel: String,
    val breedComposition: List<BreedContribution>,
    val variabilityProfile: VariabilityProfile,
    val heterosisEstimate: HeterosisEstimate?,
    val stabilizationForecast: StabilizationForecast?,
    val diversityIndicator: GeneticDiversityIndicator?,
    val selectionPressureImpact: SelectionPressureImpact?,
    val insights: List<GeneticInsight> = emptyList(),
    val summaryTags: List<String> = emptyList(),
    val assumptionsUsed: List<String> = emptyList(),
    val fallbacksUsed: List<String> = emptyList(),
    val unavailableInsights: List<GeneticInsightType> = emptyList(),
    val globalConfidence: InsightConfidence = InsightConfidence.MODERATE,
    val trace: GeneticInsightTrace? = null
)

/**
 * Debug-only observability for engine performance and logic path.
 */
data class GeneticInsightTrace(
    val contributorsExecuted: List<String>,
    val executionTimeMs: Long,
    val cacheHit: Boolean = false,
    val fallbacksTriggered: List<String> = emptyList()
)

data class VariabilityProfile(
    val uniformityScore: Double, // 0.0 (chaos) to 1.0 (clones)
    val riskLevel: VariabilityRiskLevel,
    val primaryFactors: List<String>,
    val evidence: InsightEvidence
)

data class HeterosisEstimate(
    val presence: Boolean,
    val score: Double, // 0.0 to 1.0
    val impactedTraits: List<String>,
    val evidence: InsightEvidence
)

data class StabilizationForecast(
    val progressPercentage: Int, // 0-100
    val isStagnating: Boolean = false,
    val inbreedingRisk: String? = null,
    val evidence: InsightEvidence
)

data class GeneticDiversityIndicator(
    val entropyScore: Double, // Normalized 0-100 where 100 is high diversity
    val interpretation: String, // e.g. "Broad Base", "Narrowing", "Fixed"
    val evidence: InsightEvidence
)

data class SelectionPressureImpact(
    val pressureLevel: Double, // 0.0 to 1.0
    val stabilizationSpeed: Double,
    val diversityLossRisk: Double,
    val evidence: InsightEvidence? = null
)

