package com.example.hatchtracker.domain.breeding.quant

/**
 * Identifiers for supported quantitative traits.
 */
enum class QuantitativeTraitId {
    SIZE,
    WEIGHT,
    TEMPERAMENT,
    EGG_PRODUCTION,
    GROWTH_RATE
}

/**
 * Definition of a quantitative trait's properties.
 */
data class QuantitativeTraitDefinition(
    val id: QuantitativeTraitId,
    val name: String,
    val heritability: Double, // 0.0 to 1.0
    val unit: String,
    val minDisplayValue: Double,
    val maxDisplayValue: Double
)
