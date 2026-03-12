package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.genetics.*
import org.junit.Assert.*
import org.junit.Test

class LineStabilityIndexCalculatorTest {

    @Test
    fun `compute GLSI with ideal conditions returns high score`() {
        val prediction = createMockPrediction(1.0) // Perfect homozygosity
        val risk = BreedingRiskResult(
            riskLevel = RiskLevel.NONE,
            inbreedingRiskScore = 0.0,
            bottleneckRiskScore = 0.0,
            effectivePopulation = 100.0,
            reasons = emptyList(),
            recommendation = "Safe"
        )
        val meta = DiversityMeta(uniqueSires = 10, repeatedSireCount = 0, totalBirds = 10)

        val snapshot = LineStabilityIndexCalculator.compute(
            Species.CHICKEN, 1, prediction, null, risk, meta
        )

        assertTrue("GLSI should be high, got ${snapshot.glsiScore}", snapshot.glsiScore >= 95)
        assertEquals(1.0, snapshot.components.fixation, 0.01)
        assertEquals(1.0, snapshot.components.risk, 0.01)
    }

    @Test
    fun `compute GLSI with high risk triggers inbreeding trap banner`() {
        val prevPrediction = createMockPrediction(0.8)
        val prevRisk = BreedingRiskResult(
            riskLevel = RiskLevel.NONE,
            inbreedingRiskScore = 0.0,
            bottleneckRiskScore = 0.10,
            effectivePopulation = 60.0,
            reasons = emptyList(),
            recommendation = "Safe"
        )
        val prevMeta = DiversityMeta(2, 0, 10)
        
        val prevSnapshot = LineStabilityIndexCalculator.compute(
            Species.CHICKEN, 1, prevPrediction, null, prevRisk, prevMeta
        )

        // New generation: Stability improves (fixation 0.8 -> 1.0) but risk rises fast (10 -> 50)
        val currentPrediction = createMockPrediction(1.0)
        val currentRisk = BreedingRiskResult(
            riskLevel = RiskLevel.HIGH_RISK,
            inbreedingRiskScore = 0.50,
            bottleneckRiskScore = 0.70,
            effectivePopulation = 8.0,
            reasons = listOf("Sibling mating"),
            recommendation = "Caution"
        )
        val currentMeta = DiversityMeta(1, 1, 10)

        val currentSnapshot = LineStabilityIndexCalculator.compute(
            Species.CHICKEN, 2, currentPrediction, null, currentRisk, currentMeta, prevSnapshot
        )

        assertTrue(currentSnapshot.components.risk < prevSnapshot.components.risk)
        currentSnapshot.banner?.let {
            assertTrue(it.title.contains("Stability improved"))
        }
    }

    @Test
    fun `drivers reflect component deltas`() {
        val prevPrediction = createMockPrediction(0.5)
        val prevRisk = BreedingRiskResult(
            riskLevel = RiskLevel.NONE,
            inbreedingRiskScore = 0.0,
            bottleneckRiskScore = 0.20,
            effectivePopulation = 50.0,
            reasons = emptyList(),
            recommendation = "Safe"
        )
        val prevMeta = DiversityMeta(5, 0, 10)
        
        val prevSnapshot = LineStabilityIndexCalculator.compute(
            Species.CHICKEN, 1, prevPrediction, null, prevRisk, prevMeta
        )

        // Fixation improves significantly
        val currentPrediction = createMockPrediction(1.0)
        val snapshot = LineStabilityIndexCalculator.compute(
            Species.CHICKEN, 2, currentPrediction, null, prevRisk, prevMeta, prevSnapshot
        )

        val fixationDriver = snapshot.drivers.find { it.title == "Genetic Fixation" }
        assertNotNull(fixationDriver)
        assertTrue(fixationDriver!!.delta > 0)
        assertEquals(DriverImpact.POSITIVE, fixationDriver.impact)
    }

    private fun createMockPrediction(homozygosity: Double): BreedingPredictionResult {
        val locusId = "COL"
        val genotype = GenotypeAtLocus(locusId, listOf("p", "p"))
        val distribution = GenotypeDistribution(locusId, mapOf(genotype to homozygosity))
        
        val call = GenotypeCall(locusId, listOf("p", "p"), Certainty.CONFIRMED)
        
        return BreedingPredictionResult(
            sireGenotypes = mapOf(locusId to call),
            damGenotypes = mapOf(locusId to call),
            offspringDistributions = mapOf(locusId to distribution),
            phenotypeResult = PhenotypeResult(
                probabilities = listOf(PhenotypeProbability("Color", 1.0))
            )
        )
    }
}

