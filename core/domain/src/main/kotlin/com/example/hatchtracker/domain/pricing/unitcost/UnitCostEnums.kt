package com.example.hatchtracker.domain.pricing.unitcost

enum class CostWindow {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    BATCH_LIFETIME,
    BIRD_LIFETIME,
    CUSTOM
}

enum class CostAllocationStrategy {
    BY_EGGS_PRODUCED, // Validate line production against total production
    BY_HEADCOUNT,     // Equal split per bird
    NO_ALLOCATION     // Direct costs only
}

enum class MissingData {
    EGG_COUNTS,
    COST_ENTRIES,
    LINE_NOT_DEFINED,
    BIRD_COUNT,
    HISTORICAL_DATA,
    LIVE_CHICK_COUNT
}
