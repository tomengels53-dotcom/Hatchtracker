package com.example.hatchtracker.core.common

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.FinancialStats
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.repository.BreedStandardRepository
import com.example.hatchtracker.model.PrimaryUsage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Deterministic analyzer for bird breeding performance.
 * Calculates normalized scores (0-100) based on historical data.
 */
@Singleton
class BreedingAnalyzer @Inject constructor(
    private val breedRepository: BreedStandardRepository
) {

    /**
     * Calculates a breeder score based on hatch rate, consistency, and offspring survival.
     * Weights: Hatch Rate (40%), Consistency (30%), Survival (30%).
     */
    fun calculateBreederScore(
        incubations: List<Incubation>,
        offspring: List<Bird>
    ): Int {
        val completedIncubations = incubations.filter { it.hatchCompleted }
        if (completedIncubations.isEmpty()) return 0

        // 1. Hatch Rate Component (40%)
        val totalEggs = completedIncubations.sumOf { it.eggsCount }
        val totalHatched = completedIncubations.sumOf { it.hatchedCount }
        val avgHatchRate = if (totalEggs > 0) totalHatched.toFloat() / totalEggs else 0f
        val hatchScore = (avgHatchRate * 100).coerceIn(0f, 100f)

        // 2. Consistency Component (30%)
        val rates = completedIncubations.map { 
            if (it.eggsCount > 0) it.hatchedCount.toFloat() / it.eggsCount else 0f 
        }
        val variance = if (rates.size > 1) {
            val mean = rates.average()
            rates.sumOf { (it - mean).toDouble().pow(2.0) } / (rates.size - 1)
        } else 0.0
        val stdDev = sqrt(variance).toFloat()
        val consistencyScore = (100f - (stdDev * 200f)).coerceIn(0f, 100f)

        // 3. Survival Rate Component (30%)
        val survivalScore = if (totalHatched > 0) 100f else 0f

        val finalScore = (hatchScore * 0.4f) + (consistencyScore * 0.3f) + (survivalScore * 0.3f)
        
        return finalScore.toInt().coerceIn(0, 100)
    }

    /**
     * Calculates financial insights for an incubation batch.
     */
    fun calculateFinancialInsights(
        incubation: Incubation,
        totalCosts: Double,
        breederScore: Int
    ): FinancialInsights {
        val totalEggs = incubation.eggsCount
        val costPerEgg = if (totalEggs > 0) totalCosts / totalEggs else 0.0
        val costPerHatch = if (incubation.hatchedCount > 0) totalCosts / incubation.hatchedCount else 0.0
        
        // Breed-aware base price lookup
        val breed = incubation.breeds.firstOrNull()?.let { breedRepository.getBreedById(it) }
        
        val basePricePerChick = breed?.let { b ->
            // Heuristic: Layers 5, Dual 7, Meat/Ornamental 10+
            when {
                b.normalizedPrimaryUsage.contains(PrimaryUsage.ORNAMENTAL) -> 12.0
                b.normalizedPrimaryUsage.contains(PrimaryUsage.MEAT) -> 8.0
                b.normalizedPrimaryUsage.contains(PrimaryUsage.DUAL_PURPOSE) -> 7.0
                else -> 5.0
            }
        } ?: when (incubation.species.lowercase()) {
            "chicken" -> 5.0
            "duck" -> 8.0
            "quail" -> 2.0
            "turkey" -> 15.0
            "goose" -> 25.0
            else -> 10.0
        }

        val qualityMultiplier = 1.0 + (breederScore / 200.0)
        val suggestedPrice = basePricePerChick * qualityMultiplier

        val potentialRevenue = suggestedPrice * incubation.hatchedCount
        val projectedProfit = potentialRevenue - totalCosts

        return FinancialInsights(
            costPerEgg = costPerEgg,
            costPerHatch = costPerHatch,
            suggestedPricePerChick = suggestedPrice,
            projectedProfit = projectedProfit,
            qualityBonus = (qualityMultiplier - 1.0) * 100
        )
    }

    /**
     * Calculates an aggregated summary for a list of incubations.
     */
    fun calculateHubSummary(
        incubations: List<Incubation>,
        financialStatsMap: Map<String, FinancialStats> = emptyMap()
    ): IncubationHubSummary {
        val completed = incubations.filter { it.hatchCompleted }
        if (completed.isEmpty()) return IncubationHubSummary.Empty

        val totalEggs = completed.sumOf { it.eggsCount }
        val totalInfertile = completed.sumOf { it.infertileCount }
        val totalHatched = completed.sumOf { it.hatchedCount }

        val avgFertility = com.example.hatchtracker.domain.breeding.IncubationUtils.calculateFertilityRate(totalEggs, totalInfertile)
        val avgHatchability = com.example.hatchtracker.domain.breeding.IncubationUtils.calculateHatchability(totalHatched, totalEggs, totalInfertile)

        var totalCost = 0.0
        var costedHatchCount = 0
        completed.forEach { inc ->
            val stats = financialStatsMap[inc.id.toString()]
            if (stats != null && stats.totalCost > 0) {
                totalCost += stats.totalCost
                costedHatchCount += inc.hatchedCount
            }
        }

        val avgCostPerHatch = if (costedHatchCount > 0) totalCost / costedHatchCount else 0.0
        return IncubationHubSummary(
            averageFertility = avgFertility,
            averageHatchability = avgHatchability,
            averageCostPerHatch = avgCostPerHatch,
            batchCount = completed.size,
            hasFinancialData = avgCostPerHatch > 0
        )
    }
}

data class FinancialInsights(
    val costPerEgg: Double,
    val costPerHatch: Double,
    val suggestedPricePerChick: Double,
    val projectedProfit: Double,
    val qualityBonus: Double
)

data class IncubationHubSummary(
    val averageFertility: Float,
    val averageHatchability: Float,
    val averageCostPerHatch: Double,
    val batchCount: Int,
    val hasFinancialData: Boolean
) {
    companion object {
        val Empty = IncubationHubSummary(0f, 0f, 0.0, 0, false)
    }
}
