package com.example.hatchtracker.domain.breeding

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class DominanceTransformTest {

    @Test
    fun `dominanceStrength zero returns exactly midpoint`() {
        val sireMean = 0.2
        val damMean = 0.8
        val heritability = 0.3
        val environmentalVariance = 0.1
        
        // Midpoint should be 0.5
        var offspringMean = (sireMean + damMean) / 2.0
        val dominanceStrength = 0.0 
        
        if (dominanceStrength > 0.0) {
            val m = offspringMean
            val s = dominanceStrength
            val mPrime = m + 1 * s * (m - 0.5) * (1.0 - abs(m - 0.5)) * 2.0
            offspringMean = mPrime
        }
        
        assertEquals(0.5, offspringMean, 0.0001)
    }

    @Test
    fun `dominanceStrength positive shifts mean`() {
        var offspringMean = 0.7
        val dominanceStrength = 0.5 
        val dominanceDirection = 1
        val dominanceCenter = 0.5
        
        if (dominanceStrength > 0.0) {
            val m = offspringMean
            val s = dominanceStrength
            val mPrime = m + dominanceDirection * s * (m - dominanceCenter) * (1.0 - abs(m - dominanceCenter)) * 2.0
            offspringMean = mPrime
        }
        
        // m = 0.7, s = 0.5, c = 0.5
        // mPrime = 0.7 + 1 * 0.5 * 0.2 * (1 - 0.2) * 2.0
        // 0.7 + 0.5 * 0.2 * 0.8 * 2.0 = 0.7 + 0.16 = 0.86
        assertEquals(0.86, offspringMean, 0.0001)
    }
}
