package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingTarget
import com.example.hatchtracker.data.models.TargetSex
import com.example.hatchtracker.data.models.TraitTarget
import com.example.hatchtracker.model.genetics.GenotypeAtLocus
import com.example.hatchtracker.model.genetics.GenotypeDistribution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalEvaluatorEdgeCaseTest {

    private val evaluator = BreedingGoalEvaluator()

    @Test
    fun `score drops to zero when required trait is impossible`() {
        // Given: A required trait with 0 probability in outcomes
        val outcomes = listOf(
            PhenotypeOutcome("BLUE_EGG", "BLUE_EGG", "Blue Egg", 0.0)
        )
        val target = BreedingTarget(
            requiredTraits = listOf(TraitTarget("BLUE_EGG", "BLUE_EGG"))
        )

        // When
        val score = evaluator.evaluate(outcomes, emptyList(), emptyMap(), target)

        // Then
        assertTrue("Match percentage should be near zero", score.matchPercentage < 5.0)
        assertFalse("Required traits should not be satisfied", score.satisfiedRequired)
    }

    @Test
    fun `sex-aware mismatch results in near zero score`() {
        // Given: Required trait available only in males, but target is FEMALE
        val outcomes = listOf(
            PhenotypeOutcome(
                traitId = "BARRED",
                valueId = "BARRED",
                label = "Barred",
                overallProbability = 0.5,
                maleProbability = 1.0,
                femaleProbability = 0.0
            )
        )
        val target = BreedingTarget(
            requiredTraits = listOf(TraitTarget("BARRED", "BARRED", appliesToSex = TargetSex.FEMALE))
        )

        // When
        val score = evaluator.evaluate(outcomes, emptyList(), emptyMap(), target)

        // Then
        assertFalse("Required trait should be impossible for female", score.satisfiedRequired)
    }

    @Test
    fun `homozygous selection scores higher than heterozygous due to stability`() {
        // Given: Two scenarios with same phenotype probability but different genotypes
        val outcomes = listOf(
            PhenotypeOutcome("FIBRO", "FIBRO", "Fibro", 1.0)
        )
        val target = BreedingTarget(
            requiredTraits = listOf(TraitTarget("FIBRO", "FIBRO"))
        )

        // Scenario A: Homozygous (Stable)
        val distA = mapOf(
            "FIBRO" to GenotypeDistribution(
                locusId = "FIBRO",
                outcomes = mapOf(
                    GenotypeAtLocus("FIBRO", listOf("Fm", "Fm")) to 1.0
                )
            )
        )
        val scoreA = evaluator.evaluate(outcomes, emptyList(), distA, target)

        // Scenario B: Heterozygous (Unstable)
        val distB = mapOf(
            "FIBRO" to GenotypeDistribution(
                locusId = "FIBRO",
                outcomes = mapOf(
                    GenotypeAtLocus("FIBRO", listOf("Fm", "fm")) to 1.0
                )
            )
        )
        val scoreB = evaluator.evaluate(outcomes, emptyList(), distB, target)

        // Then
        assertTrue("Stable homozygous should outscore heterozygous", scoreA.totalScore > scoreB.totalScore)
    }
}
