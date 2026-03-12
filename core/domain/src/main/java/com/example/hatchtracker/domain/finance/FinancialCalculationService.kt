package com.example.hatchtracker.domain.finance

import kotlin.math.max

/**
 * Pure domain service for all financial math in HatchBase.
 * Ensures consistent calculation of Profit, ROI, and Unit Costs across Kotlin and TypeScript.
 */
object FinancialCalculationService {

    /**
     * Profit Calculation Rule:
     * Profit = sum(revenueGross) - sum(costGross)
     * If VAT is disabled, Net = Gross.
     */
    fun computeProfitGross(revenueGross: Double, costGross: Double): Double {
        return revenueGross - costGross
    }

    fun computeProfitNet(revenueNet: Double, costNet: Double): Double {
        return revenueNet - costNet
    }

    /**
     * ROI Rule:
     * ROI = profit / totalCostNet
     * VAT is excluded from the cost basis for ROI to show true business performance.
     */
    fun computeROI(profit: Double, totalCostNet: Double): Double {
        if (totalCostNet <= 0) return 0.0
        return (profit / totalCostNet) * 100.0
    }

    /**
     * Unit Cost Calculation:
     * Logic: sum(costGross) / productionCount
     * If VAT is enabled, costBasis for unit cost is Gross (total money out).
     */
    fun computeUnitCost(totalCost: Double, unitCount: Int): Double {
        if (unitCount <= 0) return 0.0
        return totalCost / unitCount
    }

    /**
     * Break-even Calculation:
     * Units needed to cover costs = totalCosts / unitPrice
     */
    fun computeBreakEven(totalCosts: Double, unitPrice: Double): Int {
        if (unitPrice <= 0) return 0
        return kotlin.math.ceil(totalCosts / unitPrice).toInt()
    }

    /**
     * Straight-line Depreciation:
     * (Initial Value - Residual Value) / Useful Life
     */
    fun computeDepreciationStraightLine(
        initialValue: Double,
        residualValue: Double = 0.0,
        usefulLifeMonths: Int
    ): Double {
        if (usefulLifeMonths <= 0) return 0.0
        return max(0.0, (initialValue - residualValue) / usefulLifeMonths)
    }

    /**
     * Proportional Cost Allocation by count:
     * Used for splitting shared costs (e.g. feed) across multiple flocks.
     */
    fun allocateProportional(
        totalAmount: Double,
        targetCounts: List<Int>
    ): List<Double> {
        val totalCount = targetCounts.sum()
        if (totalCount <= 0) return targetCounts.map { 0.0 }
        
        return targetCounts.map { count ->
            (count.toDouble() / totalCount) * totalAmount
        }
    }
}
