package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.genetics.BreedingScenarioProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreedingEngineTest {

    @Test
    fun `GeneticRiskAnalyzer rejects cross-species pairing`() {
        val male = createMockBird(1, "Chicken", "Rhode Island Red", Sex.MALE)
        val female = createMockBird(2, "Duck", "Pekin", Sex.FEMALE)

        val risk = GeneticRiskAnalyzer.analyzeBreedingRisk(male, female, emptyList())

        assertEquals(RiskLevel.HIGH_RISK, risk.riskLevel)
        assertTrue(risk.reasons.any { it.contains("Cross-species", ignoreCase = true) })
    }

    @Test
    fun `BreedingSimulationEngine returns deterministic result for same inputs`() {
        val simulationEngine = BreedingSimulationEngine()

        val male = createMockBird(1, "Chicken", "RIR", Sex.MALE)
        val female = createMockBird(2, "Chicken", "RIR", Sex.FEMALE)
        val scenario = BreedingScenarioProfile(
            goalType = "STABILIZATION",
            breedingMode = "LINE_BREEDING",
            preferredTimeHorizonGens = 5
        )

        val firstResult = simulationEngine.simulate(
            species = Species.CHICKEN,
            sire = male,
            dam = female,
            scenario = scenario
        )
        val secondResult = simulationEngine.simulate(
            species = Species.CHICKEN,
            sire = male,
            dam = female,
            scenario = scenario
        )

        assertEquals(firstResult, secondResult)
        assertTrue(firstResult.phenotypeResult.probabilities.isEmpty())
    }

    @Test
    fun `BreedingSimulationEngine clears cache without changing result contract`() {
        val simulationEngine = BreedingSimulationEngine()

        val male = createMockBird(1, "Chicken", "Ameraucana", Sex.MALE)
        val female = createMockBird(2, "Chicken", "Ameraucana", Sex.FEMALE)
        val scenario = BreedingScenarioProfile(
            goalType = "TRAIT_SELECTION",
            breedingMode = "OUT_CROSS",
            preferredTimeHorizonGens = 3
        )

        val beforeClear = simulationEngine.simulate(
            species = Species.CHICKEN,
            sire = male,
            dam = female,
            scenario = scenario
        )
        simulationEngine.clearCache()
        val afterClear = simulationEngine.simulate(
            species = Species.CHICKEN,
            sire = male,
            dam = female,
            scenario = scenario
        )

        assertEquals(beforeClear, afterClear)
    }

    private fun createMockBird(
        id: Long,
        species: String,
        breed: String,
        sex: Sex,
        traits: List<String> = emptyList()
    ): Bird {
        return Bird(
            localId = id,
            syncId = "bird_$id",
            flockId = 1,
            species = runCatching { Species.valueOf(species.uppercase()) }.getOrDefault(Species.UNKNOWN),
            breed = breed,
            breedId = breed,
            sex = sex,
            hatchDate = "2023-01-01",
            generation = 1,
            geneticProfile = GeneticProfile(
                fixedTraits = traits,
                knownGenes = emptyList(),
                inferredTraits = emptyList()
            )
        )
    }
}
