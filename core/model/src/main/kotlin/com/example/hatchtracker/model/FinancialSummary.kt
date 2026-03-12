package com.example.hatchtracker.model

/**
 * Standard business model for a Financial Summary.
 */
data class FinancialSummary(
    val summaryId: String = "", // format: "${ownerType}_${ownerId}"
    val ownerType: String = "",
    val ownerId: String = "",
    
    val totalCosts: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val profit: Double = 0.0,
    
    val costPerEgg: Double = 0.0,
    val costPerChick: Double = 0.0,
    val costPerAdult: Double = 0.0,
    
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncTime: Long = System.currentTimeMillis()
)
