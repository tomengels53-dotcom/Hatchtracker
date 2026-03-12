package com.example.hatchtracker.domain.pricing

import com.example.hatchtracker.domain.breeding.MarketType
import kotlin.math.roundToInt

/**
 * pure domain logic for calculating prices.
 * Stateless and Currency-Agnostic.
 */
object PricingStrategist {

    data class StrategyResult(
        val suggestedPrice: Double,
        val profitPerUnit: Double,
        val assumptions: Set<AssumptionCode>
    )

    fun calculate(
        unitCost: Double,
        marginPercent: Double,
        marketType: MarketType,
        historicalAvgPrice: Double? = null
    ): StrategyResult {
        val assumptions = mutableSetOf<AssumptionCode>()

        // 1. Verification
        if (unitCost <= 0.0) {
            // Should be handled by Caller (UnitCostProvider), but safety check
            return StrategyResult(0.0, 0.0, emptySet())
        }

        // 2. Base Price (Cost + Margin)
        val marginMultiplier = 1.0 - (marginPercent / 100.0)
        var price = if (marginMultiplier > 0.01) {
            unitCost / marginMultiplier
        } else {
            unitCost * 1.5 // Safety fallback if margin is near 100%
        }

        // 3. Market Adjustment
        if (marketType != MarketType.LOCAL) {
            price *= marketType.premiumMultiplier
            assumptions.add(AssumptionCode.ADJUSTED_FOR_MARKET)
        }

        // 4. Safety Margin Check (Cost + 10%)
        val minSafe = unitCost * 1.1
        if (price < minSafe) {
            price = minSafe
            assumptions.add(AssumptionCode.ENFORCED_MIN_SAFETY_MARGIN)
        }

        // 5. Friendly Rounding
        val rounded = roundToFriendly(price)
        if (rounded != price) {
            assumptions.add(AssumptionCode.ROUNDED_TO_FRIENDLY_PRICE)
        }
        price = rounded

        // 6. Historical Comparison
        if (historicalAvgPrice != null) {
            if (price < historicalAvgPrice * 0.9) {
                assumptions.add(AssumptionCode.LOWER_THAN_HISTORICAL_AVG)
            } else if (price > historicalAvgPrice * 1.25) {
                assumptions.add(AssumptionCode.HIGHER_THAN_HISTORICAL_AVG)
            }
        }

        // 7. Impact
        val profit = price - unitCost

        return StrategyResult(
            suggestedPrice = price,
            profitPerUnit = profit,
            assumptions = assumptions
        )
    }

    private fun roundToFriendly(price: Double): Double {
        return when {
            price < 1.0 -> (price * 20).roundToInt() / 20.0 // 0.05 steps
            price < 10.0 -> (price * 2).roundToInt() / 2.0  // 0.50 steps
            price < 100.0 -> (price / 5).roundToInt() * 5.0 // 5.00 steps
            else -> (price / 10).roundToInt() * 10.0        // 10.00 steps
        }
    }
}
