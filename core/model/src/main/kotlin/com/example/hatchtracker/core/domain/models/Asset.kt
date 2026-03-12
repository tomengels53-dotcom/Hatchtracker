package com.example.hatchtracker.core.domain.models

data class Asset(
    val assetId: String,
    val name: String,
    val category: AssetCategory,
    val linkedDeviceId: String?,
    val purchaseDateEpochMs: Long,
    val purchasePrice: Double,
    val residualValue: Double,
    val depreciationMethod: DepreciationMethod,
    val usefulLifeMonths: Int?,
    val expectedCycles: Int?,
    val cyclesAllocatedCount: Int,
    val lastAllocatedAtEpochMs: Long?,
    val retiredDateEpochMs: Long?,
    val retirementValue: Double?,
    val status: AssetStatus
) {
    val depreciableBase: Double
        get() = (purchasePrice - residualValue).coerceAtLeast(0.0)
}
