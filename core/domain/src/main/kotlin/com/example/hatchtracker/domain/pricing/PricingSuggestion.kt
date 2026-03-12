package com.example.hatchtracker.domain.pricing

import com.example.hatchtracker.domain.breeding.MarketType

/**
 * Standardized pricing suggestion Output.
 * Pure data class, no logic.
 */
data class PricingSuggestion(
    val itemType: PricedItemType,
    val sourceId: String,
    
    val unitCost: Double,
    val suggestedUnitPrice: Double,
    val profitPerUnit: Double,
    
    val marginPercent: Int,
    val marketMultiplier: Double,
    val marketType: MarketType,
    
    val confidence: ConfidenceLevel,
    val breakdown: List<PricingBreakdownLine>,
    val assumptionCodes: Set<AssumptionCode>,
    val isOutsideEnvelope: Boolean = false
)

sealed class PricingSuggestionResult {
    data class Available(val suggestion: PricingSuggestion) : PricingSuggestionResult()
    data class Unavailable(
        val missingData: Set<com.example.hatchtracker.domain.pricing.unitcost.MissingData>, 
        val message: String
    ) : PricingSuggestionResult()
}
