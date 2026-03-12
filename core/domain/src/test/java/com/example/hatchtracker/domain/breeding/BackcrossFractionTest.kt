package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.model.Species
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackcrossFractionTest {

    private lateinit var architect: StagedCrossArchitect
    private val generationEstimator = mockk<GenerationEstimator>()
    private val breedingFacade = mockk<BreedingFacade>()

    @Before
    fun setup() {
        architect = StagedCrossArchitect(generationEstimator, breedingFacade)
        every { generationEstimator.estimateForStage(any(), any(), any()) } returns GenEstimate(1, 1, EstimateConfidence.HIGH, emptyList())
        every { generationEstimator.aggregate(any()) } returns GenEstimate(5, 5, EstimateConfidence.HIGH, emptyList())
    }

    @Test
    fun `donor fraction decays by half each backcross until threshold`() {
        val baseLine = BaseLineDefinition("Base", "f1", true, 0.05)
        val goals = listOf(GoalSpec(TraitDomain.EGG_TRAITS, "T", "V", 5))
        
        val donor = com.example.hatchtracker.model.BreedStandard(
            name = "Donor",
            geneticProfile = com.example.hatchtracker.model.GeneticProfile(knownGenes = listOf("V"))
        )
        every { breedingFacade.getBreedsForSpecies(any()) } returns listOf(donor)

        val roadmap = architect.createRoadmap(Species.CHICKEN, baseLine, goals, StrategyMode.STRICT_LINE_BREEDING)
        
        val f1 = roadmap.stages.find { it.type == RoadmapStageType.INTROGRESS }!!
        assertEquals(0.5, f1.donorFractionAfter, 0.001)

        val backcrosses = roadmap.stages.filter { it.type == RoadmapStageType.BACKCROSS }.sortedBy { it.stageIndex }
        assertTrue("Should have 4 backcrosses", backcrosses.size == 4)

        assertEquals(0.25, backcrosses[0].donorFractionAfter, 0.001)
        assertEquals(0.125, backcrosses[1].donorFractionAfter, 0.001)
        assertEquals(0.0625, backcrosses[2].donorFractionAfter, 0.001)
        assertEquals(0.03125, backcrosses[3].donorFractionAfter, 0.001)
    }
}
