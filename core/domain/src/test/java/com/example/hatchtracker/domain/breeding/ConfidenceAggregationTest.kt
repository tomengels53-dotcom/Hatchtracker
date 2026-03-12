package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.EstimateConfidence
import com.example.hatchtracker.data.models.GenEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.pow

class ConfidenceAggregationTest {

    @Test
    fun `aggregate calculates cube-root geometric mean correctly`() {
        val estimator = GenerationEstimator()
        
        val stages = listOf(
            GenEstimate(1, 2, EstimateConfidence.HIGH, emptyList()),
            GenEstimate(2, 3, EstimateConfidence.MED, emptyList())
        )
        
        // stageConfidences: HIGH = 0.95, MED = 0.70
        // Cm = (0.95 + 0.70) / 2 = 0.825
        
        // Cp: stagePenalty = (2-1)*0.05 = 0.05. Cp = 1.0 - 0.05 = 0.95
        // Ce: default 1.0
        
        // cTotal = pow(1.0 * 0.825 * 0.95, 1/3) = pow(0.78375, 0.333) = ~0.922
        // Since 0.922 > 0.8, returns HIGH
        
        val result = estimator.aggregate(stages)
        assertEquals(EstimateConfidence.HIGH, result.confidence)
        
        val breakdown = result.confidenceBreakdown
        assertNotNull(breakdown)
        assertEquals(1.0, breakdown!!.evidence, 0.0001)
        assertEquals(0.825, breakdown.model, 0.0001)
        assertEquals(0.95, breakdown.planComplexity, 0.0001)
    }
}
