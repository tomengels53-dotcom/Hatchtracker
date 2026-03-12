package com.example.hatchtracker.core.domain.models

data class AssetAllocationEvent(
    val allocationId: String,
    val assetId: String,
    val scopeType: AssetScopeType,
    val scopeId: String,
    val periodKey: String,
    val amount: Double,
    val createdAt: Long
)
