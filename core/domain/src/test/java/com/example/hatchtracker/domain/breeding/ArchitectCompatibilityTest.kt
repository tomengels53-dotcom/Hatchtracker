package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.model.Species
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ArchitectCompatibilityTest {

    private lateinit var architect: StagedCrossArchitect
    private val generationEstimator = mockk<GenerationEstimator>()
    private val breedingFacade = mockk<BreedingFacade>()

    @Before
    fun setup() {
        architect = StagedCrossArchitect(generationEstimator, breedingFacade)
        every { generationEstimator.estimateForStage(any(), any(), any()) } returns GenEstimate(1, 2, EstimateConfidence.HIGH, emptyList())
        every { generationEstimator.aggregate(any()) } returns GenEstimate(3, 6, EstimateConfidence.MED, emptyList())
    }

    @Test
    fun `multi-breed roadmap still builds successfully`() {
        val baseLine = BaseLineDefinition(
            baseBreed = "Leghorn",
            baseFlockId = "flock_zero",
            preserveIdentity = true,
            identityTolerance = 0.125
        )
        val goals = listOf(GoalSpec(TraitDomain.EGG_TRAITS, "O_Locus", "O", 5))

        val ameraucana = com.example.hatchtracker.model.BreedStandard(
            name = "Ameraucana",
            geneticProfile = com.example.hatchtracker.model.GeneticProfile(
                knownGenes = listOf("O")
            )
        )
        every { breedingFacade.getBreedsForSpecies("CHICKEN") } returns listOf(ameraucana)

        val roadmap = architect.createRoadmap(Species.CHICKEN, baseLine, goals, StrategyMode.STRICT_LINE_BREEDING)

        assertNotNull(roadmap)
        assertNotNull(roadmap.stages)
    }
}
