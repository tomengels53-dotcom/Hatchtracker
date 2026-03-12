package com.example.hatchtracker.data.models

import com.example.hatchtracker.model.Species

enum class StageType {
    SETUP_VALIDATION,
    PAIRING_AND_SET,
    INCUBATION_AND_HATCH,
    OFFSPRING_EVALUATION,
    KEEPER_SELECTION,
    NEXT_CROSS_PLANNING,
    STABILITY_CONFIRMATION
}

enum class ActionType {
    TASK, CHECK, INSIGHT
}

enum class EntityType {
    BIRD, FLOCK, HATCH, PAIRING
}

enum class NoteScope {
    PROGRAM, STAGE, BIRD
}

enum class LinkReason {
    EXPLICIT_STAGE_ENTITY,
    PARENT_LINEAGE,
    FLOCK_MEMBERSHIP,
    NOT_LINKED
}

data class EvidenceQuality(
    val sampleSize: Int = 0,
    val coverageScore: Int = 0, // 0-100
    val freshness: Long = 0,
    val linkConfidence: Confidence = Confidence.LOW,
    val linkReasons: Map<LinkReason, Int> = emptyMap()
)

enum class Confidence {
    LOW, MED, HIGH
}

enum class ProgramHealthStatus {
    INSUFFICIENT_DATA,
    ON_TRACK,
    DRIFT,
    OFF_TRACK
}

data class StageAction(
    val actionId: String,
    val labelKey: String,
    val type: ActionType = ActionType.TASK,
    val linkedEntityType: EntityType? = null,
    val linkedEntityId: String? = null,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

data class MeasurementSpec(
    val key: String,
    val label: String,
    val expectedValue: String? = null,
    val unit: String? = null
)

data class RiskFlag(
    val key: String,
    val severity: String, // info, warning, critical
    val message: String
)

data class CompletionCriteria(
    val requiredActions: List<String> = emptyList(),
    val requiredMeasurements: List<String> = emptyList(),
    val minSampleSize: Int = 0
)

data class ExecutionStage(
    val stageId: String,
    val type: StageType,
    val titleKey: String,
    val descriptionKey: String,
    val generationIndexHint: Int? = null,
    val actions: List<StageAction> = emptyList(),
    val measurements: List<MeasurementSpec> = emptyList(),
    val risks: List<RiskFlag> = emptyList(),
    val completion: CompletionCriteria = CompletionCriteria(),
    val isComplete: Boolean = false
)

data class TaggedTrait(
    val key: String,
    val value: String,
    val confidence: Confidence? = null
)

data class ExecutionNote(
    val noteId: String,
    val scope: NoteScope,
    val stageId: String? = null,
    val entityType: EntityType? = null,
    val entityId: String? = null,
    val text: String,
    val taggedTraits: List<TaggedTrait> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class GoalProgress(
    val domain: TraitDomain,
    val score0to100: Int,
    val evidenceCount: Int,
    val note: String? = null
)

data class ProgramHealth(
    val status: ProgramHealthStatus,
    val confidence: Confidence,
    val limitingFactors: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProgramExecutionMetadata(
    val summaryRationale: String? = null,
    val strategyConfig: StrategyConfig? = null,
    val executionStages: List<ExecutionStage> = emptyList(),
    val executionNotes: List<ExecutionNote> = emptyList(),
    val programHealth: ProgramHealth? = null,
    val goalProgress: List<GoalProgress> = emptyList(),
    val dynamicGenEstimate: GenEstimate? = null
)

data class ExecutionDashboardState(
    val program: BreedingProgram,
    val currentStage: ExecutionStage?,
    val nextActions: List<StageAction>,
    val whatsGood: List<String>,
    val needsAttention: List<String>,
    val suggestedAdjustments: List<String>,
    val diagnostics: ExecutionDiagnostics? = null
)

data class ExecutionDiagnostics(
    val stateHash: Int,
    val lastRecomputeMs: Long,
    val lastPersistAt: Long,
    val telemetrySampleSize: Int,
    val evidenceQuality: EvidenceQuality,
    val linkReasons: Map<LinkReason, Int>
)

data class TelemetrySnapshot(
    val sampleSize: Int,
    val observedExpressionByKey: Map<String, Double> = emptyMap(),
    val lastEventAt: Long = 0L
)

