package com.example.hatchtracker.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculationServiceTest {

    @Test
    fun testComputeProfitGross() {
        val profit = FinancialCalculationService.computeProfitGross(100.0, 40.0)
        assertEquals(60.0, profit, 0.001)
    }

    @Test
    fun testComputeROI() {
        val roi = FinancialCalculationService.computeROI(50.0, 100.0)
        assertEquals(50.0, roi, 0.001)
    }

    @Test
    fun testComputeROI_ZeroCost() {
        val roi = FinancialCalculationService.computeROI(50.0, 0.0)
        assertEquals(0.0, roi, 0.001)
    }

    @Test
    fun testComputeUnitCost() {
        val unitCost = FinancialCalculationService.computeUnitCost(100.0, 10)
        assertEquals(10.0, unitCost, 0.001)
    }

    @Test
    fun testComputeDepreciation_StraightLine() {
        val monthly = FinancialCalculationService.computeDepreciationStraightLine(1200.0, 0.0, 12)
        assertEquals(100.0, monthly, 0.001)
    }

    @Test
    fun testComputeDepreciation_WithSalvage() {
        val monthly = FinancialCalculationService.computeDepreciationStraightLine(1200.0, 200.0, 12)
        assertEquals(83.333, monthly, 0.001)
    }
}
