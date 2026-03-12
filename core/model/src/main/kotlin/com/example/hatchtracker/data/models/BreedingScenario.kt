package com.example.hatchtracker.data.models

enum class ScenarioStatus {
    DRAFT, PLANNED, ACTIVE, INCUBATING, EVALUATED, ARCHIVED
}

enum class ScenarioEntryMode {
    FORWARD, ASSISTED, SCRATCH
}

data class BreedingScenario(
    val id: String = "",
    val ownerUserId: String = "",
    val name: String = "",
    val species: String = "Chicken",
    val status: ScenarioStatus = ScenarioStatus.DRAFT,
    val entryMode: ScenarioEntryMode = ScenarioEntryMode.FORWARD,
    val target: BreedingTarget? = null,
    
    // Core Configuration
    val breedIds: List<String> = emptyList(),
    val traitPriorities: Map<String, ScenarioTraitConfig> = emptyMap(), // TraitId -> Config
    
    // Multi-generation simulation
    val generations: List<ScenarioGeneration> = emptyList(),
    
    // Dynamic Insights
    val riskWarnings: List<ScenarioRisk> = emptyList(),
    // Lineage & Forking
    val parentScenarioId: String? = null,
    val originalAuthorAnonymized: String? = null, // e.g. "MasterBreeder7"
    
    // Community readiness (Not yet live)
    val communityOriginalId: String? = null,
    val isPublic: Boolean = false,
    val downloadCount: Int = 0,
    val currentIncubationId: Long? = null,
    val confidenceScore: Float = 0.5f, // 0.0 to 1.0
    val geneticStabilityRatio: Float = 0.0f, // Ratio of predicted vs actual trait matches
    val volatilityIndicators: Map<String, Float> = emptyMap(), // TraitId -> Variance score
    
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

data class ScenarioTraitConfig(
    val priority: Int, // 1-5
    val weight: Float // 0.0-1.0
)

data class ScenarioGeneration(
    val generationIndex: Int, // 1 (F1), 2 (F2)...
    val status: String = "PLANNED",
    val description: String = "",
    val pairings: List<ScenarioPairing> = emptyList(),
    val predictedOutcomes: List<String> = emptyList(),
    val aiGuidanceSnapshot: String? = null
)

data class ScenarioPairing(
    val maleSource: String, // "BreedId" or "PreviousGenOutcomeId"
    val femaleSource: String,
    val rationale: String = ""
)

data class ScenarioRisk(
    val severity: String, // Low, Medium, High, Lethal
    val traitId: String,
    val description: String
)

data class ScenarioPath(
    val name: String,
    val description: String,
    val estimatedGenerations: Int
)

