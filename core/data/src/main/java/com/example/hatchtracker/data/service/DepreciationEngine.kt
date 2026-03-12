package com.example.hatchtracker.data.service

import com.example.hatchtracker.core.domain.models.Asset
import com.example.hatchtracker.core.domain.models.DepreciationMethod

object DepreciationEngine {

    /**
     * Calculates the depreciation amount for a single cycle.
     * Capped so accumulated depreciation never exceeds depreciableBase.
     */
    fun calculateCycleDepreciation(asset: Asset): Double {
        if (asset.depreciationMethod != DepreciationMethod.CYCLE_BASED) return 0.0
        val cyclesTotal = asset.expectedCycles ?: return 0.0
        if (cyclesTotal <= 0) return 0.0
        
        val base = asset.depreciableBase
        val depPerCycle = base / cyclesTotal
        
        // Capping logic (assuming cyclesAllocatedCount tracks how many times we've allocated)
        val accumulated = depPerCycle * asset.cyclesAllocatedCount
        val maxRemaining = base - accumulated
        
        return depPerCycle.coerceAtMost(maxRemaining).coerceAtLeast(0.0)
    }

    /**
     * Calculates the standard monthly depreciation amount.
     * Services consuming this should cap the final allocation value so it
     * doesn't exceed the depreciable base over time.
     */
    fun calculateMonthlyDepreciation(asset: Asset): Double {
        if (asset.depreciationMethod != DepreciationMethod.TIME_BASED) return 0.0
        val totalMonths = asset.usefulLifeMonths ?: return 0.0
        if (totalMonths <= 0) return 0.0
        
        val base = asset.depreciableBase
        return (base / totalMonths).coerceAtLeast(0.0)
    }
}
