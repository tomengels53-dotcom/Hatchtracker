package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingProgram
import com.example.hatchtracker.data.models.BreedingProgramStatus
import com.example.hatchtracker.data.models.Confidence
import com.example.hatchtracker.data.models.EvidenceQuality
import com.example.hatchtracker.data.models.ExecutionDashboardState
import com.example.hatchtracker.data.models.ExecutionDiagnostics
import com.example.hatchtracker.data.models.ExecutionNote
import com.example.hatchtracker.data.models.ExecutionStage
import com.example.hatchtracker.data.models.LinkReason
import com.example.hatchtracker.data.models.ProgramHealth
import com.example.hatchtracker.data.models.ProgramHealthStatus
import com.example.hatchtracker.data.models.StageAction
import com.example.hatchtracker.data.models.StageType
import com.example.hatchtracker.data.models.TelemetrySnapshot
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.genetics.Certainty
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(FlowPreview::class)
class ProgramExecutionEngine @Inject constructor(
    private val programRepository: BreedingProgramRepository,
    private val birdRepository: BirdRepository,
    private val generationEstimator: GenerationEstimator
) {
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lastPersistTime = MutableStateFlow(0L)
    private val stateToPersist = MutableSharedFlow<ExecutionDashboardState>(extraBufferCapacity = 1)

    private data class HysteresisState(val status: ProgramHealthStatus, val count: Int)
    private val hysteresisMap = mutableMapOf<String, HysteresisState>()
    private val telemetryCache = ConcurrentHashMap<String, Pair<Int, TelemetryResult>>()

    init {
        engineScope.launch {
            stateToPersist
                .debounce(800)
                .distinctUntilChangedBy { it.diagnostics?.stateHash ?: 0 }
                .collect { state ->
                    programRepository.updatePlan(state.program)
                    lastPersistTime.value = System.currentTimeMillis()
                }
        }
    }

    fun observeExecution(programId: String): Flow<ExecutionDashboardState> {
        return combine(
            programRepository.observePlans("").map { plans -> plans.find { it.id == programId } },
            birdRepository.allBirds
        ) { program: BreedingProgram?, birds: List<Bird> ->
            if (program == null) return@combine null

            val startedAt = System.currentTimeMillis()
            val telemetry = computeTelemetry(program, birds)
            val updatedProgram = reconcileProgramState(program, telemetry)
            val currentStage = updatedProgram.executionStages.firstOrNull { !it.isComplete }
                ?: updatedProgram.executionStages.lastOrNull()

            val state = ExecutionDashboardState(
                program = updatedProgram,
                currentStage = currentStage,
                nextActions = currentStage?.actions?.filter { !it.isDone }?.take(3).orEmpty(),
                whatsGood = computeInsights(updatedProgram, telemetry.snapshot, "GOOD"),
                needsAttention = computeInsights(updatedProgram, telemetry.snapshot, "ATTENTION"),
                suggestedAdjustments = computeInsights(updatedProgram, telemetry.snapshot, "ADJUST"),
                diagnostics = ExecutionDiagnostics(
                    stateHash = computeStateHash(updatedProgram),
                    lastRecomputeMs = System.currentTimeMillis() - startedAt,
                    lastPersistAt = lastPersistTime.value,
                    telemetrySampleSize = telemetry.snapshot.sampleSize,
                    evidenceQuality = telemetry.quality,
                    linkReasons = telemetry.linkReasons
                )
            )

            stateToPersist.tryEmit(state)
            state
        }
            .filterNotNull()
            .flowOn(Dispatchers.Default)
            .distinctUntilChangedBy { it.diagnostics?.stateHash ?: 0 }
    }

    suspend fun toggleActionDone(programId: String, stageId: String, actionId: String, done: Boolean) {
        val program = programRepository.getPlan(programId).getOrNull() ?: return
        val updatedStages = program.executionStages.map { stage ->
            if (stage.stageId != stageId) return@map stage
            stage.copy(
                actions = stage.actions.map { action ->
                    if (action.actionId != actionId) return@map action
                    action.copy(
                        isDone = done,
                        completedAt = if (done) System.currentTimeMillis() else null
                    )
                }
            )
        }
        programRepository.updatePlan(
            program.copy(
                executionStages = updatedStages,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addNote(programId: String, note: ExecutionNote) {
        val program = programRepository.getPlan(programId).getOrNull() ?: return
        programRepository.updatePlan(
            program.copy(
                executionNotes = program.executionNotes + note,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun computeTelemetry(
        program: BreedingProgram,
        allBirds: List<Bird>
    ): TelemetryResult {
        val linkedIds = program.linkedAssets.map { it.refId }.toSet()

        // Fast pre-filter for cache hash
        val relevantBirds = allBirds.filter { bird ->
             linkedIds.contains(bird.cloudId) ||
             linkedIds.contains(bird.motherId?.toString()) ||
             linkedIds.contains(bird.fatherId?.toString()) ||
             linkedIds.contains(bird.flockId?.toString())
        }.take(200)

        val birdHash = relevantBirds.sumOf { it.localId + it.lastUpdated.hashCode() }
        val programHash = program.strategyConfig?.goalSpecs.hashCode()
        val combinedHash = birdHash.hashCode() * 31 + programHash

        val cached = telemetryCache[program.id]
        if (cached != null && cached.first == combinedHash) {
            return cached.second
        }

        val linkReasons = mutableMapOf<LinkReason, Int>()
        relevantBirds.forEach { bird ->
            val reason = when {
                linkedIds.contains(bird.cloudId) -> LinkReason.EXPLICIT_STAGE_ENTITY
                linkedIds.contains(bird.motherId?.toString()) || linkedIds.contains(bird.fatherId?.toString()) -> LinkReason.PARENT_LINEAGE
                linkedIds.contains(bird.flockId?.toString()) -> LinkReason.FLOCK_MEMBERSHIP
                else -> LinkReason.NOT_LINKED
            }
            if (reason != LinkReason.NOT_LINKED) {
                linkReasons[reason] = (linkReasons[reason] ?: 0) + 1
            }
        }

        var hasFirmEvidence = false
        val observed = mutableMapOf<String, Double>()
        val goals = program.strategyConfig?.goalSpecs.orEmpty()
        goals.forEach { goal ->
            val matches = relevantBirds.count { bird ->
                var birdScore = 0.0
                
                // Check if Quantitative Trait
                val quantVal = bird.geneticProfile.quantitativeTraits[goal.traitKey]
                if (quantVal != null) {
                    val targetDouble = goal.targetValue.toDoubleOrNull()
                    if (targetDouble != null) {
                        // Using a 10% tolerance for 'matching' a quantitative target
                        val tolerance = targetDouble * 0.1
                        val distance = kotlin.math.abs(quantVal.mean - targetDouble)
                        if (distance <= tolerance || distance < 0.5) { // fallback absolute tolerance
                            val isFixed = quantVal.variance < 0.1 // Variance threshold for fixation
                            birdScore = if (isFixed) 1.0 else 0.6
                            if (isFixed) hasFirmEvidence = true
                        }
                    }
                } else {
                    // Check Mendelian Trait
                    val call = bird.geneticProfile.genotypeCalls?.get(goal.traitKey)
                    if (call != null && call.alleles.contains(goal.targetValue)) {
                        birdScore = when (call.certainty) {
                            Certainty.CONFIRMED -> 1.0
                            Certainty.ASSUMED -> 0.8
                            Certainty.UNKNOWN -> 0.5
                        }
                        if (call.certainty == Certainty.CONFIRMED) hasFirmEvidence = true
                    }
                }
                // Also consider trait overrides as a strong signal
                val overrideMatch = bird.geneticProfile.traitOverrides.any { override ->
                    override.traitId == goal.traitKey && override.optionId == goal.targetValue
                }
                if (overrideMatch) {
                    birdScore = 1.0
                    hasFirmEvidence = true
                }
                birdScore > 0.0 // Count as a match if score is positive
            }
            if (relevantBirds.isNotEmpty()) {
                observed[goal.traitKey] = matches.toDouble() / relevantBirds.size.toDouble()
            }
        }

        val sampleSize = relevantBirds.size
        val quality = EvidenceQuality(
            sampleSize = sampleSize,
            coverageScore = if (goals.isEmpty()) 100 else (observed.size * 100) / goals.size,
            freshness = relevantBirds.maxOfOrNull { it.lastUpdated } ?: 0L,
            linkConfidence = when {
                (linkReasons[LinkReason.EXPLICIT_STAGE_ENTITY] ?: 0) > 0 -> {
                    hasFirmEvidence = true
                    Confidence.HIGH
                }
                (linkReasons[LinkReason.PARENT_LINEAGE] ?: 0) > 0 -> Confidence.MED
                else -> Confidence.LOW
            },
            linkReasons = linkReasons
        )

        val result = TelemetryResult(
            snapshot = TelemetrySnapshot(
                sampleSize = sampleSize,
                observedExpressionByKey = observed,
                lastEventAt = System.currentTimeMillis()
            ),
            quality = quality,
            linkReasons = linkReasons,
            hasConfirmedEvidence = hasFirmEvidence
        )

        telemetryCache[program.id] = combinedHash to result
        return result
    }

    private fun reconcileProgramState(program: BreedingProgram, telemetry: TelemetryResult): BreedingProgram {
        var current = program
        if (current.executionStages.isEmpty() && current.status == BreedingProgramStatus.ACTIVE) {
            current = current.copy(executionStages = generateInitialStages())
        }

        val rawStatus = when {
            telemetry.snapshot.sampleSize == 0 -> ProgramHealthStatus.INSUFFICIENT_DATA
            checkDrift(current, telemetry.snapshot, 0.5) -> ProgramHealthStatus.OFF_TRACK
            checkDrift(current, telemetry.snapshot, 0.2) -> ProgramHealthStatus.DRIFT
            else -> ProgramHealthStatus.ON_TRACK
        }
        val finalStatus = applyHysteresis(current.id, current.programHealth?.status, rawStatus)

        val health = ProgramHealth(
            status = finalStatus,
            confidence = telemetry.quality.linkConfidence,
            limitingFactors = computeLimitingFactors(telemetry.snapshot),
            updatedAt = System.currentTimeMillis()
        )

        val observedAverage = telemetry.snapshot.observedExpressionByKey.values.average().takeIf { !it.isNaN() } ?: 0.0
        val dynamicEstimate = current.strategyConfig?.let { config ->
            generationEstimator.estimate(
                config = config,
                observedExpression = observedAverage,
                sampleSize = telemetry.snapshot.sampleSize
            )
        }

        return current.copy(
            programHealth = health,
            dynamicGenEstimate = dynamicEstimate,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun computeInsights(
        program: BreedingProgram,
        telemetry: TelemetrySnapshot,
        bucket: String
    ): List<String> {
        val goals = program.strategyConfig?.goalSpecs.orEmpty()
        if (goals.isEmpty()) {
            return when (bucket) {
                "GOOD" -> listOf("Program is active and waiting for execution data.")
                "ATTENTION" -> listOf("No strategy goals are configured for this plan.")
                else -> listOf("Define goal specs to enable targeted execution guidance.")
            }
        }

        val items = goals.map { goal ->
            val observed = telemetry.observedExpressionByKey[goal.traitKey] ?: 0.0
            goal to observed
        }

        return when (bucket) {
            "GOOD" -> items
                .filter { (_, observed) -> observed >= 0.5 }
                .map { (goal, observed) -> "${goal.traitKey} tracking well (${(observed * 100).toInt()}%)." }
                .ifEmpty { listOf("Linked evidence captured for ${telemetry.sampleSize} records.") }
            "ATTENTION" -> items
                .filter { (_, observed) -> observed < 0.3 }
                .map { (goal, observed) -> "${goal.traitKey} below target (${(observed * 100).toInt()}%)." }
                .ifEmpty { listOf("No critical drift signals detected.") }
            else -> items
                .sortedBy { (_, observed) -> observed }
                .take(3)
                .map { (goal, _) -> "Prioritize candidate selection for ${goal.traitKey}." }
                .ifEmpty { listOf("Continue logging outcomes to refine recommendations.") }
        }.take(5)
    }

    private fun computeLimitingFactors(telemetry: TelemetrySnapshot): List<String> {
        val factors = mutableListOf<String>()
        if (telemetry.sampleSize < 3) factors += "Low sample size for execution telemetry"
        val lowTraits = telemetry.observedExpressionByKey.count { it.value < 0.2 }
        if (lowTraits > 0) factors += "$lowTraits target traits showing low expression"
        return if (factors.isEmpty()) listOf("No major limiting factors detected") else factors.take(3)
    }

    private fun applyHysteresis(
        programId: String,
        current: ProgramHealthStatus?,
        proposed: ProgramHealthStatus
    ): ProgramHealthStatus {
        if (current == null || current == proposed) {
            hysteresisMap.remove(programId)
            return proposed
        }
        if (current == ProgramHealthStatus.INSUFFICIENT_DATA) {
            hysteresisMap.remove(programId)
            return proposed
        }
        if (proposed != ProgramHealthStatus.DRIFT && proposed != ProgramHealthStatus.OFF_TRACK) {
            hysteresisMap.remove(programId)
            return proposed
        }

        val existing = hysteresisMap[programId]
        return if (existing != null && existing.status == proposed) {
            if (existing.count >= 3) {
                hysteresisMap.remove(programId)
                proposed
            } else {
                hysteresisMap[programId] = existing.copy(count = existing.count + 1)
                current
            }
        } else {
            hysteresisMap[programId] = HysteresisState(proposed, 1)
            current
        }
    }

    private fun checkDrift(program: BreedingProgram, telemetry: TelemetrySnapshot, threshold: Double): Boolean {
        if (telemetry.sampleSize < 3) return false
        return program.strategyConfig?.goalSpecs.orEmpty().any { goal ->
            val observed = telemetry.observedExpressionByKey[goal.traitKey] ?: 0.0
            observed < threshold
        }
    }

    private fun computeStateHash(program: BreedingProgram): Int {
        return java.util.Objects.hash(
            program.id,
            program.status,
            program.programHealth?.status,
            program.dynamicGenEstimate?.minGenerations,
            program.dynamicGenEstimate?.maxGenerations,
            program.executionNotes.size,
            program.executionStages.sortedBy { it.stageId }.map { stage ->
                java.util.Objects.hash(
                    stage.stageId,
                    stage.isComplete,
                    stage.actions.sortedBy { it.actionId }.map { it.isDone }
                )
            }
        )
    }

    private data class TelemetryResult(
        val snapshot: TelemetrySnapshot,
        val quality: EvidenceQuality,
        val linkReasons: Map<LinkReason, Int>,
        val hasConfirmedEvidence: Boolean
    )

    private fun generateInitialStages(): List<ExecutionStage> {
        return listOf(
            ExecutionStage(
                stageId = "setup",
                type = StageType.SETUP_VALIDATION,
                titleKey = "Base Population Validation",
                descriptionKey = "Ensure starting birds meet initial trait requirements.",
                actions = listOf(
                    StageAction(actionId = "validate_sires", labelKey = "Check sire genetic profiles"),
                    StageAction(actionId = "validate_dams", labelKey = "Check dam genetic profiles"),
                    StageAction(actionId = "confirm_health", labelKey = "Verify biosecurity and health status")
                )
            ),
            ExecutionStage(
                stageId = "pairing",
                type = StageType.PAIRING_AND_SET,
                titleKey = "Initial Pairings",
                descriptionKey = "Set up first generation matings according to strategy.",
                actions = listOf(
                    StageAction(actionId = "assign_pairs", labelKey = "Assign individual matings"),
                    StageAction(actionId = "set_eggs", labelKey = "Collect and set eggs for incubation")
                )
            )
        )
    }
}
