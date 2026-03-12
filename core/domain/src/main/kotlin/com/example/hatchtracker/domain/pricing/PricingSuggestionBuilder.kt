package com.example.hatchtracker.domain.pricing

import com.example.hatchtracker.domain.breeding.MarketType
import com.example.hatchtracker.domain.pricing.unitcost.UnitCostResult
import javax.inject.Inject

class PricingSuggestionBuilder @Inject constructor() {

    fun build(
        costResult: UnitCostResult,
        marginPercent: Double,
        marketType: MarketType,
        itemType: PricedItemType,
        sourceId: String,
        species: com.example.hatchtracker.model.Species,
        historicalAvg: Double? = null
    ): PricingSuggestionResult {
        
        return when (costResult) {
            is UnitCostResult.Unavailable -> {
                PricingSuggestionResult.Unavailable(costResult.missingData, costResult.message)
            }
            is UnitCostResult.Available -> {
                val strategy = PricingStrategist.calculate(
                    unitCost = costResult.unitCost,
                    marginPercent = marginPercent,
                    marketType = marketType,
                    historicalAvgPrice = historicalAvg
                )

                val category = when (itemType) {
                    PricedItemType.EGG -> PricingCategory.EGG
                    PricedItemType.CHICK -> PricingCategory.CHICK
                    PricedItemType.ADULT -> PricingCategory.ADULT
                }
                
                val envelope = MarketEnvelopeConfig.getEnvelope(species, category)
                val priceCents = (strategy.suggestedPrice * 100).toLong()
                val isOutside = priceCents < envelope.minCents || priceCents > envelope.maxCents

                val allAssumptions = if (isOutside) {
                    costResult.assumptions + strategy.assumptions + AssumptionCode.OUTSIDE_MARKET_ENVELOPE
                } else {
                    costResult.assumptions + strategy.assumptions
                }
                
                val confidence = calculateConfidence(allAssumptions)

                PricingSuggestionResult.Available(
                    PricingSuggestion(
                        itemType = itemType,
                        sourceId = sourceId,
                        unitCost = costResult.unitCost,
                        suggestedUnitPrice = strategy.suggestedPrice,
                        profitPerUnit = strategy.profitPerUnit,
                        marginPercent = marginPercent.toInt(),
                        marketMultiplier = marketType.premiumMultiplier,
                        marketType = marketType,
                        confidence = confidence,
                        breakdown = costResult.breakdown,
                        assumptionCodes = allAssumptions,
                        isOutsideEnvelope = isOutside
                    )
                )
            }
        }
    }

    private fun calculateConfidence(assumptions: Set<AssumptionCode>): ConfidenceLevel {
        // High = Real data, minimal fuzz
        // Medium = Some allocation logic or estimates
        // Low = Default values or heavy guessing
        if (assumptions.contains(AssumptionCode.USED_DEFAULT_VALUES)) return ConfidenceLevel.LOW
        if (assumptions.contains(AssumptionCode.COST_ALLOCATED_BY_EGGS)) return ConfidenceLevel.MEDIUM
        return ConfidenceLevel.HIGH
    }
}

