package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.FinancialEntry
import kotlin.math.abs
import kotlin.math.sqrt

data class ForecastResult(
    val totalCost: Double,
    val costPerEgg: Double,
    val costPerChick: Double,
    val assumedHatchRate: Float,
    val confidenceInterval: Double // Simple variance indicator
)

data class HistoricalPoint(
    val incubation: Incubation,
    val entries: List<FinancialEntry>
) {
    val totalCost: Double = entries.sumOf { it.amount }
    val costPerEgg: Double = if (incubation.eggsCount > 0) totalCost / incubation.eggsCount else 0.0
}

object HatchForecaster {

    /**
     * Predicts costs for a future hatch.
     * @param plannedEggs Number of eggs to set
     * @param plannedDurationDays (Not strictly used yet as most costs are per-egg or fixed, but here for future expansion)
     * @param assumedHatchRate User-adjustable hatch rate assumption (0-100)
     * @param history List of completed incubations with their associated costs
     */
    fun predict(
        plannedEggs: Int,
        plannedDurationDays: Int,
        assumedHatchRate: Float,
        history: List<HistoricalPoint>
    ): ForecastResult {
        if (history.isEmpty()) {
            return ForecastResult(0.0, 0.0, 0.0, assumedHatchRate, 0.0)
        }

        // 1. Filter out failures and outliers
        val validPoints = history.filter {
            !it.incubation.failedCount.equals(it.incubation.eggsCount) && // Not a total failure
            it.incubation.eggsCount > 0 &&
            it.totalCost > 0
        }

        if (validPoints.isEmpty()) {
            return ForecastResult(0.0, 0.0, 0.0, assumedHatchRate, 0.0)
        }

        // 2. Outlier Detection (Z-score approach on cost-per-egg)
        val costsPerEgg = validPoints.map { it.costPerEgg }
        val meanCostPerEgg = costsPerEgg.average()
        val stdDev = calculateStdDev(costsPerEgg, meanCostPerEgg)
        
        val filteredPoints = if (validPoints.size > 3) {
            validPoints.filter { abs(it.costPerEgg - meanCostPerEgg) <= 2 * stdDev }
        } else {
            validPoints
        }

        // 3. Weighted Average (Recent hatches count more)
        // Weight = Index + 1 (where higher index is more recent)
        var totalWeight = 0.0
        var weightedCostPerEgg = 0.0
        
        filteredPoints.sortedBy { it.incubation.startDate }.forEachIndexed { index, point ->
            val weight = (index + 1).toDouble()
            weightedCostPerEgg += point.costPerEgg * weight
            totalWeight += weight
        }

        val finalCostPerEgg = weightedCostPerEgg / totalWeight
        val predictedTotalCost = finalCostPerEgg * plannedEggs
        
        val expectedChicks = (plannedEggs * (assumedHatchRate / 100f)).coerceAtLeast(1f)
        val predictedCostPerChick = predictedTotalCost / expectedChicks

        return ForecastResult(
            totalCost = predictedTotalCost,
            costPerEgg = finalCostPerEgg,
            costPerChick = predictedCostPerChick,
            assumedHatchRate = assumedHatchRate,
            confidenceInterval = stdDev
        )
    }

    private fun calculateStdDev(values: List<Double>, mean: Double): Double {
        if (values.size < 2) return 0.0
        val sumSq = values.sumOf { (it - mean) * (it - mean) }
        return sqrt(sumSq / (values.size - 1))
    }
}

