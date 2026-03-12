package com.example.hatchtracker.domain.pricing.unitcost

import com.example.hatchtracker.domain.pricing.AssumptionCode
import com.example.hatchtracker.domain.pricing.PricingBreakdownLine

sealed class UnitCostResult {
    data class Available(
        val unitCost: Double,
        val totalCost: Double,
        val totalUnits: Int,
        val breakdown: List<PricingBreakdownLine>,
        val assumptions: Set<AssumptionCode>,
        // Refined Cents (Phase 2)
        val unitCostCents: Long = (unitCost * 100).toLong(),
        val totalCostCents: Long = (totalCost * 100).toLong()
    ) : UnitCostResult()

    data class Unavailable(
        val missingData: Set<MissingData>,
        val message: String
    ) : UnitCostResult()
}
