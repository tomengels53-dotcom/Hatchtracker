package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.core.domain.R

enum class MarketType(val displayNameRes: Int, val premiumMultiplier: Double) {
    LOCAL(R.string.finance_market_local, 1.0),
    BREEDER(R.string.finance_market_breeder, 1.5),
    WHOLESALE(R.string.finance_market_wholesale, 0.75)
}
