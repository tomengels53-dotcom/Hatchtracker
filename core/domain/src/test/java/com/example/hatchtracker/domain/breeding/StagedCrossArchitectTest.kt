package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.BreedStandard
import com.example.hatchtracker.model.GeneticProfile
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StagedCrossArchitectTest {

    private lateinit var architect: StagedCrossArchitect
    private val generationEstimator = mockk<GenerationEstimator>()
    private val breedingFacade = mockk<BreedingFacade>()

    @Before
    fun setup() {
        architect = StagedCrossArchitect(generationEstimator, breedingFacade)
        
        // Default mock for estimator
        every { generationEstimator.estimateForStage(any(), any(), any()) } returns GenEstimate(1, 2, EstimateConfidence.HIGH, emptyList())
        every { generationEstimator.aggregate(any()) } returns GenEstimate(3, 6, EstimateConfidence.MED, emptyList())
        
        // Default mock for repository
        every { breedingFacade.getBreedsForSpecies(any()) } returns emptyList()
    }

    @Test
    fun `scoreDonor penalizes trait overload in STRICT mode`() {
        val ameraucana = BreedStandard(
            name = "Ameraucana",
            geneticProfile = GeneticProfile(
                knownGenes = listOf("O", "pr"),
                fixedTraits = listOf("pea_comb", "muffs_beard")
            )
        )
        val goals = listOf(
            GoalSpec(TraitDomain.EGG_TRAITS, "O_Locus", "O", 5),
            GoalSpec(TraitDomain.PLUMAGE_TRAITS, "MUFFS_BEARDS", "muffs_beard", 4),
            GoalSpec(TraitDomain.STRUCTURAL_TRAITS, "PEA_COMB", "pea_comb", 3)
        )

        val strictScore = architect.scoreDonor(ameraucana, goals, StrategyMode.STRICT_LINE_BREEDING)
        val commercialScore = architect.scoreDonor(ameraucana, goals, StrategyMode.COMMERCIAL_PRODUCTION)

        // Strict should be lower due to acquisition penalty (>2 dominant traits)
        assertTrue("Strict score ($strictScore) should be less than commercial ($commercialScore)", strictScore < commercialScore)
    }

    @Test
    fun `createRoadmap schedules BACKCROSS in STRICT mode`() {
        val baseLine = BaseLineDefinition(
            baseBreed = "Leghorn",
            baseFlockId = "f1",
            preserveIdentity = true,
            identityTolerance = 0.125
        )
        val goals = listOf(GoalSpec(TraitDomain.EGG_TRAITS, "O_Locus", "O", 5))
        
        val ameraucana = BreedStandard(
            name = "Ameraucana",
            geneticProfile = GeneticProfile(
                knownGenes = listOf("O")
            )
        )
        every { breedingFacade.getBreedsForSpecies("CHICKEN") } returns listOf(ameraucana)

        val roadmap = architect.createRoadmap(Species.CHICKEN, baseLine, goals, StrategyMode.STRICT_LINE_BREEDING)

        assertTrue(roadmap.stages.any { it.type == RoadmapStageType.BACKCROSS })
        assertTrue(roadmap.stages.any { it.type == RoadmapStageType.FIXATION })
    }

    @Test
    fun `createRoadmap schedules STABILIZE in COMMERCIAL mode`() {
        val baseLine = BaseLineDefinition(
            baseBreed = "Leghorn",
            baseFlockId = "f1",
            preserveIdentity = false
        )
        val goals = listOf(GoalSpec(TraitDomain.EGG_TRAITS, "O_Locus", "O", 5))

        val ameraucana = BreedStandard(
            name = "Ameraucana",
            geneticProfile = GeneticProfile(
                knownGenes = listOf("O")
            )
        )
        every { breedingFacade.getBreedsForSpecies("CHICKEN") } returns listOf(ameraucana)

        val roadmap = architect.createRoadmap(Species.CHICKEN, baseLine, goals, StrategyMode.COMMERCIAL_PRODUCTION)

        assertTrue(roadmap.stages.any { it.type == RoadmapStageType.INTERCROSS })
        assertTrue(roadmap.stages.any { it.type == RoadmapStageType.STABILIZE })
    }
}

