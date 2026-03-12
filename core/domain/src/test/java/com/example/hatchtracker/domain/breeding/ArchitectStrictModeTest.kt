package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.model.Species
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class ArchitectStrictModeTest {

    private lateinit var architect: StagedCrossArchitect
    private val generationEstimator = mockk<GenerationEstimator>()
    private val breedingFacade = mockk<BreedingFacade>()

    @Before
    fun setup() {
        architect = StagedCrossArchitect(generationEstimator, breedingFacade)
        every { generationEstimator.estimateForStage(any(), any(), any()) } returns GenEstimate(1, 1, EstimateConfidence.HIGH, emptyList())
        every { generationEstimator.aggregate(any()) } returns GenEstimate(5, 5, EstimateConfidence.HIGH, emptyList())
        
        val donor = com.example.hatchtracker.model.BreedStandard(
            name = "Donor",
            geneticProfile = com.example.hatchtracker.model.GeneticProfile(knownGenes = listOf("V"))
        )
        every { breedingFacade.getBreedsForSpecies(any()) } returns listOf(donor)
    }

    @Test
    fun `STRICT mode produces BC4 before FIXATION`() {
        val baseLine = BaseLineDefinition("Base", "f1", true, 0.05)
        val goals = listOf(GoalSpec(TraitDomain.EGG_TRAITS, "T", "V", 5))
        
        val roadmap = architect.createRoadmap(Species.CHICKEN, baseLine, goals, StrategyMode.STRICT_LINE_BREEDING)
        
        val backcrosses = roadmap.stages.filter { it.type == RoadmapStageType.BACKCROSS }
        assertTrue("Strict needs multiple backcrosses", backcrosses.size >= 4)
        
        val lastStage = roadmap.stages.last()
        assertTrue(lastStage.type == RoadmapStageType.FIXATION)
    }

    @Test
    fun `COMMERCIAL mode may stop earlier and uses INTERCROSS instead of BACKCROSS`() {
        val baseLine = BaseLineDefinition("Base", "f1", false, 0.5)
        val goals = listOf(GoalSpec(TraitDomain.EGG_TRAITS, "T", "V", 5))

        val roadmap = architect.createRoadmap(Species.CHICKEN, baseLine, goals, StrategyMode.COMMERCIAL_PRODUCTION)
        
        val hasBackcross = roadmap.stages.any { it.type == RoadmapStageType.BACKCROSS }
        assertFalse("Commercial mode should not enforce backcross loop", hasBackcross)

        val hasIntercross = roadmap.stages.any { it.type == RoadmapStageType.INTERCROSS }
        assertTrue("Commercial mode uses intercross", hasIntercross)

        val lastStage = roadmap.stages.last()
        assertTrue(lastStage.type == RoadmapStageType.STABILIZE)
    }
}
