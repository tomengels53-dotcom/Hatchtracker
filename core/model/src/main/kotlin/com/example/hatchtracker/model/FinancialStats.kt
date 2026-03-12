package com.example.hatchtracker.model

data class FinancialStats(
    val totalCost: Double,
    val totalRevenue: Double,
    val netProfit: Double,
    val avgROI: Double,
    val entryCount: Int,
    val costPerUnit: Double = 0.0,
    val netProfitPerUnit: Double = 0.0
)

enum class FinancialTrustLevel {
    HIGH, ESTIMATED, INSUFFICIENT
}

data class EnrichedFinancialStats(
    val baseStats: FinancialStats,
    val trustLevel: FinancialTrustLevel,
    val costPerBird: Double? = null,
    val profitPerBatch: Double? = null
)

