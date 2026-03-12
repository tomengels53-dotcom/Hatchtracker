package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TraitCovarianceTest {

    @Test
    fun `variance inflates with negative covariance and penalizes score`() {
        // Create an evaluator
        val evaluator = BreedingGoalEvaluator()
        
        val quantitativePredictions = listOf(
            QuantitativePrediction(
                traitKey = "egg_size",
                mean = 0.5,
                variance = 0.1,
                heritability = 0.3
            ),
            QuantitativePrediction(
                traitKey = "egg_count",
                mean = 0.5,
                variance = 0.1,
                heritability = 0.3
            )
        )

        val target = BreedingTarget(
            requiredTraits = listOf(
                TraitTarget("egg_size", "0.5")
            ),
            preferredTraits = listOf(
                TraitTarget("egg_count", "0.5", weight = 1.0)
            )
        )
        
        // Let's assume TraitCovarianceCatalog has -0.4 for ("egg_size", "egg_count").
        // conflictPenaltySum = 1.2 + 1.2 = 2.4
        // conflictPenaltyRaw = 0.2 * (2.4 / 2.0) = 0.2 * 1.2 = 0.24
        // Subtracted from score: 0.24 * 100 = 24.0
        
        val score = evaluator.evaluate(emptyList(), quantitativePredictions, emptyMap(), target)
        
        // Total score base = 300 (required) + 100 (preferred) = 400
        // Because means match exactly, p=1.0 for both -> base totalScore = 400.0
        // Minus conflict penalty 24.0 -> 376.0
        
        assertEquals(376.0, score.totalScore, 0.0001)
        
        // Variance penalty computation = min(0.106 * 0.5, 0.3) = 0.053
        // Combined confidence penalty = min(0.24, 0.25) + 0.053 = 0.293
        // matchPercentage = (376.0 / 400.0 * 100) * ( 1.0 - 0.293 ) = 94.0 * 0.707 = 66.458
        
        assertEquals(66.458, score.matchPercentage, 0.001)
    }
}
