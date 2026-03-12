package com.example.hatchtracker.domain.pricing

import com.example.hatchtracker.domain.pricing.unitcost.UnitCostResult

/**
 * Interface for calculating unit costs across different domains (Flocks, Incubations).
 */
interface UnitCostProvider {
    /**
     * Estimated cost to produce one egg (feed, labor, overhead).
     */
    suspend fun getEggUnitCost(flockId: String, daysBack: Int = 30): UnitCostResult

    /**
     * Estimated value/cost of a newly hatched chick.
     */
    suspend fun getChickUnitCost(incubationId: String): UnitCostResult
}
