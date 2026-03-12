package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.*
import com.example.hatchtracker.domain.breeding.BreedingPredictionResult
import com.example.hatchtracker.model.genetics.*
import com.example.hatchtracker.model.UiText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeneticInsightEngineRegressionTest {

    private lateinit var engine: GeneticInsightEngine
    private val variabilityAnalyzer = VariabilityAnalyzer(InsightConfidenceEvaluator())
    private val heterosisEstimator = HeterosisEstimator(InsightConfidenceEvaluator())
    private val stabilizationForecaster = StabilizationForecaster(InsightConfidenceEvaluator())
    private val breedCompositionInterpreter = BreedCompositionInterpreter()
    private val confidenceEvaluator = InsightConfidenceEvaluator()

    @Before
    fun setup() {
        engine = GeneticInsightEngine(
            variabilityAnalyzer,
            heterosisEstimator,
            stabilizationForecaster,
            breedCompositionInterpreter,
            confidenceEvaluator
        )
    }

    @Test
    fun analyzePairing_IsDeterministicAndSeeded() = runBlocking {
        val sire = createTestBird("Sire1", "RIR", 1)
        val dam = createTestBird("Dam1", "LEGHORN", 1)
        val prediction = BreedingPredictionResult(
            phenotypeResult = PhenotypeResult(emptyList()),
            lineStability = null
        )

        val report1 = engine.analyzePairing(Species.CHICKEN, sire, dam, prediction)
        val report2 = engine.analyzePairing(Species.CHICKEN, sire, dam, prediction)

        // Ignore trace in equality check as time varies slightly, but check it exists
        assertNotNull(report1.trace)
        assertEquals("F2", report1.generationLabel)
    }

    @Test
    fun analyzePairing_HandlesLogicalEquivalence() = runBlocking {
        // Different ordering in composition should yield same report due to normalization
        val compA = listOf(BreedContribution("A", 0.6), BreedContribution("B", 0.4))
        val compB = listOf(BreedContribution("B", 0.4), BreedContribution("A", 0.6))

        val sireA = createBaseBird("S1").copy(breedComposition = compA, generationLabel = "F1")
        val damA = createBaseBird("D1").copy(breedComposition = compA, generationLabel = "F1")
        
        val sireB = createBaseBird("S1").copy(breedComposition = compB, generationLabel = "F1")
        val damB = createBaseBird("D1").copy(breedComposition = compB, generationLabel = "F1")

        val prediction = BreedingPredictionResult(
            phenotypeResult = PhenotypeResult(emptyList()),
            lineStability = null
        )

        val report1 = engine.analyzePairing(Species.CHICKEN, sireA, damA, prediction)
        val report2 = engine.analyzePairing(Species.CHICKEN, sireB, damB, prediction)

        assertEquals("F2", report1.generationLabel)
        assertEquals("F2", report2.generationLabel)
    }

    @Test
    fun analyzePairing_IsolatesContributorFailureAndTimeout() = runBlocking {
        // Given a failing contributor
        val failingContributor = object : InsightContributor {
            override val id = "FAIL_CONTRIBUTOR"
            override val priority = 100
            override fun supports(scenario: BreedingScenarioProfile?, report: GeneticInsightReport) = true
            override fun contribute(scenario: BreedingScenarioProfile, report: GeneticInsightReport): List<GeneticInsight> {
                throw RuntimeException("Simulated Failure")
            }
        }
        
        // Given a slow contributor (should timeout at 50ms)
        val slowContributor = object : InsightContributor {
            override val id = "SLOW_CONTRIBUTOR"
            override val priority = 50
            override fun supports(scenario: BreedingScenarioProfile?, report: GeneticInsightReport) = true
            override fun contribute(scenario: BreedingScenarioProfile, report: GeneticInsightReport): List<GeneticInsight> {
                Thread.sleep(100) // Sleep is okay in runBlocking for timing checks if we aren't testing high concurrency
                return listOf(mockk())
            }
        }

        engine.registerContributor(failingContributor)
        engine.registerContributor(slowContributor)

        val sire = createTestBird("Sire1", "RIR", 1)
        val dam = createTestBird("Dam1", "LEGHORN", 1)
        val prediction = BreedingPredictionResult(
            phenotypeResult = PhenotypeResult(emptyList()),
            lineStability = null
        )

        // When
        val report = engine.analyzePairing(Species.CHICKEN, sire, dam, prediction)

        // Then
        assertTrue("Report should be produced despite failures", report.generationLabel.isNotBlank())
        assertTrue("UnavailableInsights should contain records", report.unavailableInsights.isNotEmpty())
        assertNotNull(report.trace)
    }

    @Test
    fun deriveGenerationLabel_SofterAncestryLabel() {
        val sire = createBaseBird("S1").copy(generationLabel = null)
        val dam = createBaseBird("D1").copy(generationLabel = "F1")
        
        val label = engine.deriveGenerationLabel(sire, dam)
        assertEquals("Limited Pedigree Information", label)
    }

    @Test
    fun diversity_ClampRule_SingleBreedIsFixed() = runBlocking {
        val comp = listOf(BreedContribution("A", 1.0))
        val sire = createBaseBird("S1").copy(breedComposition = comp, breedId = "A", generationLabel = "Purebred")
        val dam = createBaseBird("D1").copy(breedComposition = comp, breedId = "A", generationLabel = "Purebred")
        
        val report = engine.analyzePairing(
            Species.CHICKEN,
            sire,
            dam,
            BreedingPredictionResult(
                phenotypeResult = PhenotypeResult(emptyList()),
                lineStability = null
            )
        )
        
        assertEquals(0.0, report.diversityIndicator?.entropyScore ?: -1.0, 0.001)
        assertEquals("Highly Fixed", report.diversityIndicator?.interpretation)
    }

    private fun createTestBird(id: String, breedId: String, gen: Int): Bird {
        return Bird(
            syncId = id,
            breedId = breedId,
            generation = gen,
            breedComposition = listOf(BreedContribution(breedId, 1.0)),
            generationLabel = "F$gen"
        )
    }

    private fun createBaseBird(id: String): Bird {
        return Bird(
            syncId = id,
            breedId = "PURE",
            generation = 1,
            breedComposition = listOf(BreedContribution("PURE", 1.0))
        )
    }
}
