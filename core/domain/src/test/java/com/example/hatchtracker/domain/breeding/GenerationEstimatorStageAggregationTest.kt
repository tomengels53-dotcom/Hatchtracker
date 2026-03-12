package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import org.junit.Assert.assertEquals
import org.junit.Test

class GenerationEstimatorStageAggregationTest {

    private val estimator = GenerationEstimator()

    @Test
    fun `multi stage roadmap yields correct min max sum`() {
        val stages = listOf(
            GenEstimate(1, 1, EstimateConfidence.HIGH, listOf("F1")),
            GenEstimate(1, 1, EstimateConfidence.MED, listOf("BC1")),
            GenEstimate(2, 4, EstimateConfidence.LOW, listOf("FIX"))
        )
        val result = estimator.aggregate(stages)
        assertEquals(4, result.minGenerations)
        assertEquals(6, result.maxGenerations)
    }

    @Test
    fun `confidence decreases per stage with bounded clamp`() {
        val stages = List(10) { GenEstimate(1, 1, EstimateConfidence.LOW, emptyList()) }
        val result = estimator.aggregate(stages)
        
        val numeric = result.numericConfidence

        org.junit.Assert.assertTrue(
            "Confidence should be clamped >= 0.35 but was \$numeric",
            numeric >= 0.35
        )

        org.junit.Assert.assertTrue(
            "Confidence should not be unrealistically high for many LOW stages but was \$numeric",
            numeric < 0.7
        )

        // Validate enum mapping consistency
        assertEquals(
            EstimateConfidence.fromNumeric(numeric),
            result.confidence
        )
    }
}
