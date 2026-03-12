package com.example.hatchtracker.domain.pricing

data class PricingBreakdownLine(
    val label: String,
    val amount: Double
)

enum class AssumptionCode {
    ROUNDED_TO_FRIENDLY_PRICE,
    ENFORCED_MIN_SAFETY_MARGIN,
    ADJUSTED_FOR_MARKET,
    LOWER_THAN_HISTORICAL_AVG,
    HIGHER_THAN_HISTORICAL_AVG,
    COST_ALLOCATED_BY_EGGS,
    USED_DEFAULT_VALUES,
    EXCLUDED_CRACKED_EGGS,
    ESTIMATED_FROM_HATCH_RATE,
    OUTSIDE_MARKET_ENVELOPE
}

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}
