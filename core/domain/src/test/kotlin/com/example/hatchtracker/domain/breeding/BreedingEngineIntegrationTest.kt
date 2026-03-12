package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.BreedingGoal
import com.example.hatchtracker.model.BreedingGoalType
import com.example.hatchtracker.model.ConfidenceLevel
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.GeneticProfile
import org.junit.Assert.assertTrue
import org.junit.Test

class BreedingEngineIntegrationTest {

    private fun createBird(id: Long, motherId: Long? = null, fatherId: Long? = null, traits: List<String> = emptyList()): Bird {
        return Bird(
            localId = id,
            syncId = "bird_$id",
            species = Species.CHICKEN,
            breed = "TestBreed",
            sex = if (id % 2 == 0L) Sex.MALE else Sex.FEMALE,
            hatchDate = "2025-01-01",
            motherId = motherId,
            fatherId = fatherId,
            geneticProfile = GeneticProfile(
                fixedTraits = traits,
                confidenceLevel = ConfidenceLevel.HIGH.name
            )
        )
    }

    @Test
    fun testCOICalculation() {
        // Pedigree:
        // 1 (M), 2 (F) -> 3 (M), 4 (F) [Full Sibs]
        // 3, 4 -> 5 (Offspring of full siblings)
        
        val b1 = createBird(1)
        val b2 = createBird(2)
        val b3 = createBird(3, motherId = 2, fatherId = 1)
        val b4 = createBird(4, motherId = 2, fatherId = 1)
        
        val birdMap = listOf(b1, b2, b3, b4).associateBy { it.localId }
        
        val coi = AncestryService.calculateCOI(b3, b4, birdMap)
        assertTrue(coi > 0f)
        
        // Half Siblings:
        // 1, 2 -> 3
        // 1, 6 -> 7
        val b6 = createBird(6)
        val b7 = createBird(7, motherId = 6, fatherId = 1)
        val birdMapHalf = listOf(b1, b2, b3, b6, b7).associateBy { it.localId }
        val coiHalf = AncestryService.calculateCOI(b3, b7, birdMapHalf)
        assertTrue(coiHalf > 0f)
        assertTrue(coi >= coiHalf)
    }

    @Test
    fun testMultiGenBeamSearch() {
        // Goal: Breed an "Olive Egger"
        // Start: Blue layer (O), Brown layer (BR)
        val b1 = createBird(1, traits = listOf("O")) // Male Blue
        val b2 = createBird(2, traits = listOf("BR")) // Female Brown
        
        val goals = listOf(
            BreedingGoal(type = BreedingGoalType.EGG_COLOR, targetValue = "O", priority = 1),
            BreedingGoal(type = BreedingGoalType.EGG_COLOR, targetValue = "BR", priority = 1)
        )
        
        val paths = BreedingOptimizer.runMultiGenBeamSearch(
            initialPopulation = listOf(b1, b2),
            goals = goals,
            maxGens = 2,
            beamWidth = 5
        )
        
        assertTrue("Should find at least one path", paths.isNotEmpty())
        val bestPath = paths.first()
        
        // The first generation (F1) should already have both traits in our simplified simulation
        assertTrue(bestPath.steps.size in 1..2)
        assertTrue(bestPath.steps.last().offspring.geneticProfile.fixedTraits.contains("O"))
        assertTrue(bestPath.steps.last().offspring.geneticProfile.fixedTraits.contains("BR"))
    }
}

