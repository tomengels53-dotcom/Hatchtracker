package com.example.hatchtracker.data.models

data class BreedingProgram(
    val id: String = "",
    val ownerUserId: String = "",
    val scenarioId: String = "",
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: BreedingProgramStatus = BreedingProgramStatus.ACTIVE,
    val steps: List<BreedingProgramStep> = emptyList(),
    val linkedAssets: List<BreedingProgramAssetLink> = emptyList(),
    val planSpecies: com.example.hatchtracker.model.Species = com.example.hatchtracker.model.Species.CHICKEN,
    val finalGeneration: Int = 1,
    val activeGenerationIndex: Int = 1,
    val mergeMode: MergeMode = MergeMode.KEEP_SEPARATE,
    val auditLog: List<BreedingProgramAuditEntry> = emptyList(),
    
    // Unified metadata from legacy scenario if applicable
    val entryMode: ScenarioEntryMode = ScenarioEntryMode.FORWARD,
    val target: BreedingTarget? = null,
    val summaryRationale: String? = null,
    val strategyConfig: StrategyConfig? = null,
    
    // Phase 3 Execution fields
    val executionStages: List<ExecutionStage> = emptyList(),
    val executionNotes: List<ExecutionNote> = emptyList(),
    val programHealth: ProgramHealth? = null,
    val goalProgress: List<GoalProgress> = emptyList(),
    val dynamicGenEstimate: GenEstimate? = null
)

