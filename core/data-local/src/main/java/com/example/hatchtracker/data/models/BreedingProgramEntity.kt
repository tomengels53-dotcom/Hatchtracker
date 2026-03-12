package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "breeding_action_plans")
data class BreedingProgramEntity(
    @PrimaryKey
    val syncId: String,
    val ownerUserId: String,
    val scenarioId: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: BreedingProgramStatus,
    val steps: List<BreedingProgramStep>,
    val lastModified: Long,
    val syncState: SyncState = SyncState.SYNCED,
    val syncError: String? = null,
    val cloudUpdatedAt: Long? = null,
    val deleted: Boolean = false,
    
    // Unified metadata from legacy scenario if applicable
    val linkedAssets: List<BreedingProgramAssetLink> = emptyList(),
    val planSpecies: com.example.hatchtracker.model.Species = com.example.hatchtracker.model.Species.CHICKEN,
    val finalGeneration: Int = 1,
    val activeGenerationIndex: Int = 1,
    val mergeMode: MergeMode = MergeMode.KEEP_SEPARATE,
    val auditLog: List<BreedingProgramAuditEntry> = emptyList(),
    val entryMode: ScenarioEntryMode = ScenarioEntryMode.FORWARD,
    val target: BreedingTarget? = null,
    val summaryRationale: String? = null
)

fun BreedingProgramEntity.toDomainModel(): BreedingProgram {
    val gson = com.google.gson.Gson()
    val metadata = summaryRationale?.let {
        try {
            gson.fromJson(it, ProgramExecutionMetadata::class.java)
        } catch (e: Exception) {
            // Fallback for legacy plain text summary
            ProgramExecutionMetadata(summaryRationale = it)
        }
    } ?: ProgramExecutionMetadata()

    return BreedingProgram(
        id = syncId,
        ownerUserId = ownerUserId,
        scenarioId = scenarioId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        status = status,
        steps = steps,
        linkedAssets = linkedAssets,
        planSpecies = planSpecies,
        finalGeneration = finalGeneration,
        activeGenerationIndex = activeGenerationIndex,
        mergeMode = mergeMode,
        auditLog = auditLog,
        entryMode = entryMode,
        target = target,
        summaryRationale = metadata.summaryRationale,
        strategyConfig = metadata.strategyConfig,
        executionStages = metadata.executionStages,
        executionNotes = metadata.executionNotes,
        programHealth = metadata.programHealth,
        goalProgress = metadata.goalProgress,
        dynamicGenEstimate = metadata.dynamicGenEstimate
    )
}

fun BreedingProgram.toEntity(): BreedingProgramEntity {
    val gson = com.google.gson.Gson()
    val metadata = ProgramExecutionMetadata(
        summaryRationale = summaryRationale,
        strategyConfig = strategyConfig,
        executionStages = executionStages,
        executionNotes = executionNotes,
        programHealth = programHealth,
        goalProgress = goalProgress,
        dynamicGenEstimate = dynamicGenEstimate
    )
    val metadataJson = gson.toJson(metadata)

    return BreedingProgramEntity(
        syncId = id,
        ownerUserId = ownerUserId,
        scenarioId = scenarioId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        status = status,
        steps = steps,
        lastModified = updatedAt,
        linkedAssets = linkedAssets,
        planSpecies = planSpecies,
        finalGeneration = finalGeneration,
        activeGenerationIndex = activeGenerationIndex,
        mergeMode = mergeMode,
        auditLog = auditLog,
        entryMode = entryMode,
        target = target,
        summaryRationale = metadataJson
    )
}

