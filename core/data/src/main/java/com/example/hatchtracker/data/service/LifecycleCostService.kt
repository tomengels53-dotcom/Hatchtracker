package com.example.hatchtracker.data.service

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.models.CostBasisLedgerEntryEntity
import com.example.hatchtracker.core.domain.models.LedgerEntityType
import com.example.hatchtracker.core.domain.models.LedgerSourceType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifecycleCostService @Inject constructor(
    private val database: AppDatabase
) {
    /**
     * Records a frozen cost basis for an entity.
     * This creates an audit snapshot in the cost_basis_ledger table.
     */
    suspend fun recordFrozenBasis(
        entityType: LedgerEntityType,
        entityId: String,
        amountCents: Long,
        sourceType: LedgerSourceType,
        userId: String
    ): String {
        val entry = CostBasisLedgerEntryEntity(
            entityType = entityType,
            entityId = entityId,
            sourceType = sourceType,
            amount = amountCents.toDouble() / 100.0, // Ledger stores Double for broad compatibility
            ownerUserId = userId
        )
        database.financialDao().insertOrUpdateCostBasisEntry(entry) // Need to add this to FinancialDao
        return entry.syncId
    }
}
