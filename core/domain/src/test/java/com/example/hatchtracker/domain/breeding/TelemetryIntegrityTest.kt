package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.GeneticProfile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TelemetryIntegrityTest {

    private lateinit var engine: ProgramExecutionEngine
    private val programRepository = mockk<BreedingProgramRepository>()
    private val birdRepository = mockk<BirdRepository>()
    private val generationEstimator = mockk<GenerationEstimator>()

    @Before
    fun setup() {
        engine = ProgramExecutionEngine(programRepository, birdRepository, generationEstimator)
    }

    @Test
    fun `observeExecution publishes diagnostics for linked birds`() = runBlocking {
        val stage = ExecutionStage(
            stageId = "stage_1",
            type = StageType.PAIRING_AND_SET,
            titleKey = "Test",
            descriptionKey = "Test",
            actions = listOf(StageAction("a1", "Do it", isDone = true))
        )
        val program = BreedingProgram(
            id = "prog_1",
            status = BreedingProgramStatus.ACTIVE,
            linkedAssets = listOf(
                BreedingProgramAssetLink(
                    type = AssetType.BIRD,
                    refId = "bird_1",
                    role = LinkRole.BREEDER_POOL,
                    generationIndex = null
                )
            ),
            strategyConfig = StrategyConfig(
                goalSpecs = listOf(GoalSpec(TraitDomain.EGG_TRAITS, "O_Locus", "O"))
            ),
            executionStages = listOf(stage)
        )

        val bird = Bird(
            localId = 1L,
            syncId = "bird_1",
            species = Species.CHICKEN,
            breed = "Test",
            hatchDate = "2025-01-01",
            cloudId = "bird_1",
            geneticProfile = GeneticProfile()
        )

        every { programRepository.observePlans(any()) } returns flowOf(listOf(program))
        every { birdRepository.allBirds } returns flowOf(listOf(bird))
        every { generationEstimator.estimate(any(), any(), any()) } returns GenEstimate(1, 2, EstimateConfidence.HIGH, emptyList())
        coEvery { programRepository.updatePlan(any()) } returns Result.success(Unit)

        val state = engine.observeExecution("prog_1").first()

        assertNotNull(state.diagnostics)
        assertTrue(state.diagnostics!!.telemetrySampleSize >= 1)
        assertEquals(ProgramHealthStatus.ON_TRACK, state.program.programHealth?.status)
    }
}
