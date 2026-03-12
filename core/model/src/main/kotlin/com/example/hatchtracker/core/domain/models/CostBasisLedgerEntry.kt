package com.example.hatchtracker.core.domain.models

data class CostBasisLedgerEntry(
    val entryId: String,
    val entityType: LedgerEntityType,
    val entityId: String,
    val sourceType: LedgerSourceType,
    val amount: Double,
    val createdAt: Long
)
