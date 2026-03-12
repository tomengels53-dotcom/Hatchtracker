package com.example.hatchtracker.data.repository

import com.example.hatchtracker.domain.pricing.*
import com.example.hatchtracker.domain.pricing.unitcost.UnitCostResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of UnitCostProvider that uses repository data.
 */
@Singleton
class UnitCostProviderImpl @Inject constructor(
    private val financialRepository: FinancialRepository,
    private val eggProductionDao: com.example.hatchtracker.data.EggProductionDao,
    private val incubationRepository: IncubationRepository,
    private val assetDao: com.example.hatchtracker.data.AssetDao
) : UnitCostProvider {

    override suspend fun getEggUnitCost(flockId: String, daysBack: Int): UnitCostResult {
        val now = System.currentTimeMillis()
        val startTime = now - (daysBack.toLong() * 24 * 60 * 60 * 1000L)
        
        // 1. Get total production cost for this flock in period (Optimized Long math)
        val totalCostsCents = financialRepository.getSumCents(
            ownerType = "flock",
            ownerId = flockId,
            startDate = startTime,
            endDate = now,
            type = "cost"
        )
        
        // 2. Get total eggs produced in period
        val startEpoch = startTime / (24 * 60 * 60 * 1000L)
        val endEpoch = now / (24 * 60 * 60 * 1000L)
        val totalEggs = eggProductionDao.getSumTotalEggs(flockId, startEpoch, endEpoch, null) ?: 0
        
        val assumptions = mutableSetOf<AssumptionCode>()
        if (totalEggs <= 0) {
            assumptions.add(AssumptionCode.USED_DEFAULT_VALUES)
            return UnitCostResult.Available(
                unitCost = 0.15,
                unitCostCents = 15,
                totalCost = 0.0,
                totalCostCents = 0L,
                totalUnits = 0,
                breakdown = listOf(PricingBreakdownLine("Estimated/Baseline", 0.15)),
                assumptions = assumptions
            )
        }
        
        val calculatedCents = totalCostsCents / totalEggs
        val finalUnitCostCents = if (calculatedCents > 0) calculatedCents else 15L
        if (calculatedCents <= 0) assumptions.add(AssumptionCode.USED_DEFAULT_VALUES)
        
        return UnitCostResult.Available(
            unitCost = finalUnitCostCents.toDouble() / 100.0,
            unitCostCents = finalUnitCostCents,
            totalCost = totalCostsCents.toDouble() / 100.0,
            totalCostCents = totalCostsCents,
            totalUnits = totalEggs,
            breakdown = listOf(
                PricingBreakdownLine("Historical Costs ($daysBack days)", totalCostsCents.toDouble() / 100.0),
                PricingBreakdownLine("Total Eggs Produced", totalEggs.toDouble())
            ),
            assumptions = assumptions
        )
    }

    override suspend fun getChickUnitCost(incubationId: String): UnitCostResult {
        val incubation = incubationRepository.getIncubationBySyncId(incubationId) 
            ?: return UnitCostResult.Available(2.50, 0.0, 0, emptyList(), setOf(AssumptionCode.USED_DEFAULT_VALUES))
        
        // 0. Use frozen basis if available
        if (incubation.isCostFrozen) {
            val totalInvestmentCents = incubation.costBasisCents
            val hatchedCount = incubation.hatchedCount
            val unitCostCents = if (hatchedCount > 0) totalInvestmentCents / hatchedCount else 0L
            
            return UnitCostResult.Available(
                unitCost = unitCostCents.toDouble() / 100.0,
                unitCostCents = unitCostCents,
                totalCost = totalInvestmentCents.toDouble() / 100.0,
                totalCostCents = totalInvestmentCents,
                totalUnits = hatchedCount,
                breakdown = listOf(PricingBreakdownLine("Frozen Basis (Finalized)", totalInvestmentCents.toDouble() / 100.0)),
                assumptions = emptySet()
            )
        }

        // 1. Get incubation specific costs
        val specificCostsCents = financialRepository.getSumCents(
            ownerType = "incubation",
            ownerId = incubationId,
            startDate = 0,
            endDate = Long.MAX_VALUE,
            type = "cost"
        )
        
        // 2. Get egg value (cost of eggs set)
        val flockIdStr = incubation.flockId?.toString() ?: ""
        val eggCostResult = if (flockIdStr.isNotEmpty()) getEggUnitCost(flockIdStr) else null
        val eggCostCents = (eggCostResult as? UnitCostResult.Available)?.unitCostCents ?: 15L
        val eggsValueCents = incubation.eggsCount * eggCostCents
        
        // 3. Asset Depreciation (Deterministic allocation)
        val assetAllocationCents = calculateDepreciationAllocation(incubation)
        
        val totalHatched = incubation.hatchedCount
        val totalInvestmentCents = specificCostsCents + eggsValueCents + assetAllocationCents
        
        val assumptions = mutableSetOf<AssumptionCode>()
        if (eggCostResult == null || (eggCostResult is UnitCostResult.Available && eggCostResult.assumptions.contains(AssumptionCode.USED_DEFAULT_VALUES))) {
            assumptions.add(AssumptionCode.USED_DEFAULT_VALUES)
        }

        if (totalHatched <= 0) {
            return UnitCostResult.Available(
                unitCost = 2.50,
                unitCostCents = 250,
                totalCost = totalInvestmentCents.toDouble() / 100.0,
                totalCostCents = totalInvestmentCents,
                totalUnits = 0,
                breakdown = listOf(
                    PricingBreakdownLine("Total Investment", totalInvestmentCents.toDouble() / 100.0),
                    PricingBreakdownLine("Hatched Count", 0.0)
                ),
                assumptions = assumptions + AssumptionCode.USED_DEFAULT_VALUES
            )
        }
        
        val calculatedCents = totalInvestmentCents / totalHatched
        val finalUnitCostCents = if (calculatedCents > 0) calculatedCents else 250L
        if (calculatedCents <= 0) assumptions.add(AssumptionCode.USED_DEFAULT_VALUES)
        
        return UnitCostResult.Available(
            unitCost = finalUnitCostCents.toDouble() / 100.0,
            unitCostCents = finalUnitCostCents,
            totalCost = totalInvestmentCents.toDouble() / 100.0,
            totalCostCents = totalInvestmentCents,
            totalUnits = totalHatched,
            breakdown = listOf(
                PricingBreakdownLine("Egg Input Value", eggsValueCents.toDouble() / 100.0),
                PricingBreakdownLine("Incubation Expenses", specificCostsCents.toDouble() / 100.0),
                PricingBreakdownLine("Asset Depreciation", assetAllocationCents.toDouble() / 100.0),
                PricingBreakdownLine("Chicks Hatched", totalHatched.toDouble())
            ),
            assumptions = assumptions
        )
    }

    private suspend fun calculateDepreciationAllocation(incubation: com.example.hatchtracker.data.models.Incubation): Long {
        // Simple time-based depreciation for v1
        val incubatorId = incubation.incubatorDeviceId
        if (incubatorId.isEmpty()) return 0L
        
        val asset = assetDao.getActiveAssetByDeviceId(incubatorId) ?: return 0L
        
        val purchasePriceCents = (asset.purchasePrice * 100).toLong()
        val residualValueCents = (asset.residualValue * 100).toLong()
        val depreciableBaseCents = (purchasePriceCents - residualValueCents).coerceAtLeast(0)
        
        val usefulLifeMonths = asset.usefulLifeMonths ?: 60 // Default 5 years
        val usefulLifeDays = usefulLifeMonths * 30L
        
        // For chickens, assume 21 day incubation. In a real app, calculate actual duration.
        val incubationDaysUsed = 21L 
        
        return (depreciableBaseCents * incubationDaysUsed) / usefulLifeDays
    }
}
