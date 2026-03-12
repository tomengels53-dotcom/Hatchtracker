package com.example.hatchtracker.domain.pricing

import com.example.hatchtracker.model.Species

enum class PricingCategory {
    EGG, CHICK, ADULT
}

data class MarketEnvelope(
    val minCents: Long,
    val maxCents: Long
)

object MarketEnvelopeConfig {
    private val envelopes = mapOf(
        Pair(Species.CHICKEN, PricingCategory.EGG) to MarketEnvelope(10, 200),
        Pair(Species.CHICKEN, PricingCategory.CHICK) to MarketEnvelope(100, 1500),
        Pair(Species.CHICKEN, PricingCategory.ADULT) to MarketEnvelope(500, 4500),
        
        Pair(Species.DUCK, PricingCategory.EGG) to MarketEnvelope(50, 400),
        Pair(Species.DUCK, PricingCategory.CHICK) to MarketEnvelope(300, 2500),
        
        Pair(Species.GOOSE, PricingCategory.EGG) to MarketEnvelope(200, 1000),
        Pair(Species.GOOSE, PricingCategory.CHICK) to MarketEnvelope(1000, 5000),
        
        Pair(Species.TURKEY, PricingCategory.EGG) to MarketEnvelope(100, 600)
    )

    fun getEnvelope(species: Species, category: PricingCategory): MarketEnvelope {
        return envelopes[species to category] ?: MarketEnvelope(0, 10000) // Conservative default
    }
}

