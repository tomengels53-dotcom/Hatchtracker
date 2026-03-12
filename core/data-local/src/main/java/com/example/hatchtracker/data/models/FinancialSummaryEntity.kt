package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_summaries")
data class FinancialSummaryEntity(
    @PrimaryKey
    val summaryId: String, // format: "${ownerType}_${ownerId}"
    val ownerType: String,
    val ownerId: String,
    
    val totalCosts: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val profit: Double = 0.0,
    
    val costPerEgg: Double = 0.0,
    val costPerChick: Double = 0.0,
    val costPerAdult: Double = 0.0,
    
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncTime: Long = System.currentTimeMillis()
)

