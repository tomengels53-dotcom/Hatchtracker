package com.example.hatchtracker.data.mappers

import com.example.hatchtracker.data.models.FinancialSummaryEntity
import com.example.hatchtracker.model.FinancialSummary

fun FinancialSummaryEntity.toModel(): FinancialSummary {
    return FinancialSummary(
        summaryId = summaryId,
        ownerType = ownerType,
        ownerId = ownerId,
        totalCosts = totalCosts,
        totalRevenue = totalRevenue,
        profit = profit,
        costPerEgg = costPerEgg,
        costPerChick = costPerChick,
        costPerAdult = costPerAdult,
        updatedAt = updatedAt,
        lastSyncTime = lastSyncTime
    )
}

fun FinancialSummary.toEntity(): FinancialSummaryEntity {
    return FinancialSummaryEntity(
        summaryId = summaryId,
        ownerType = ownerType,
        ownerId = ownerId,
        totalCosts = totalCosts,
        totalRevenue = totalRevenue,
        profit = profit,
        costPerEgg = costPerEgg,
        costPerChick = costPerChick,
        costPerAdult = costPerAdult,
        updatedAt = updatedAt,
        lastSyncTime = lastSyncTime
    )
}
