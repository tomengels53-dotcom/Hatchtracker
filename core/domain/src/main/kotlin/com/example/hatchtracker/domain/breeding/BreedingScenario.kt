package com.example.hatchtracker.domain.breeding

enum class ScenarioStatus {
    DRAFT, PLANNED, ACTIVE, INCUBATING, EVALUATED, ARCHIVED
}

data class BreedingScenario(
    val id: String = "",
    val ownerUserId: String = "",
    val name: String = "",
    val species: String = "Chicken",
    val status: ScenarioStatus = ScenarioStatus.DRAFT,
    
    // Core Configuration
    val breedIds: List<String> = emptyList(),
    val traitPriorities: Map<String, ScenarioTraitConfig> = emptyMap(), // TraitId -> Config
    
    // Multi-generation simulation
    val generations: List<ScenarioGeneration> = emptyList(),
    
    // Dynamic Insights
    val riskWarnings: List<ScenarioRisk> = emptyList(),
    
    // Lineage & Forking
    val parentScenarioId: String? = null,
    val originalAuthorAnonymized: String? = null,
    
    // Community readiness 
    val communityOriginalId: String? = null,
    val isPublic: Boolean = false,
    val downloadCount: Int = 0,
    val currentIncubationId: Long? = null,
    val confidenceScore: Float = 0.5f,
    val geneticStabilityRatio: Float = 0.0f,
    val volatilityIndicators: Map<String, Float> = emptyMap(),
    
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
    val maleSource: String,
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
