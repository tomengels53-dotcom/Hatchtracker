package com.example.hatchtracker.core.domain.models

enum class AssetCategory {
    INCUBATOR,
    HATCHER,
    BROODER,
    COOP,
    CARE,
    MONITORING,
    OTHER
}


enum class DepreciationMethod {
    TIME_BASED,
    CYCLE_BASED
}

enum class AssetStatus {
    ACTIVE,
    RETIRED
}

enum class AssetScopeType {
    GLOBAL,
    FLOCK,
    INCUBATION,
    FLOCKLET,
    SALES_BATCH,
    INDIVIDUAL_BIRD
}

enum class LedgerEntityType {
    INCUBATION,
    FLOCKLET,
    BIRD
}

enum class LedgerSourceType {
    INCUBATOR_DEPRECIATION,
    BROODER_DEPRECIATION,
    COOP_DEPRECIATION,
    DIRECT_COST,
    SALE_COGS,
    MORTALITY_ADJUSTMENT,
    GRADUATION_TRANSFER,
    HATCHER_DEPRECIATION
}

enum class LossHandlingPolicy {
    REDISTRIBUTE_TO_SURVIVORS,
    WRITE_OFF_LOSS
}
