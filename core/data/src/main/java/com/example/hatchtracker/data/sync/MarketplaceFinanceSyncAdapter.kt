package com.example.hatchtracker.data.sync

import com.example.hatchtracker.data.models.FinancialEntry
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.domain.model.MarketplaceSale
import com.example.hatchtracker.core.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter to project Marketplace sales into the Financial domain.
 * Ensures idempotency using the Sale ID as the syncId.
 */
@Singleton
class MarketplaceFinanceSyncAdapter @Inject constructor(
    private val financialRepository: FinancialRepository
) {
    private val TAG = "MarketplaceFinanceSync"

    /**
     * Records a marketplace sale as revenue in the finance domain.
     * Idempotent by checking for existing syncId (Sale ID).
     */
    suspend fun syncSaleToFinance(sale: MarketplaceSale): Result<Unit> {
        return try {
            // 1. Check for idempotency
            val existing = financialRepository.getEntryBySyncId(sale.id)
            if (existing != null) {
                Logger.d(TAG, "Sale ${sale.id} already synced. Skipping.")
                return Result.success(Unit)
            }

            // 2. Map linked entities to finance owners
            // For now, we take the first linked entity as the primary owner for revenue
            val primaryEntity = sale.linkedEntities.firstOrNull()
            val ownerId = primaryEntity?.entityId ?: sale.sellerUserId
            val ownerType = primaryEntity?.entityType?.lowercase() ?: "user"

            // 3. Create financial entry
            val entry = FinancialEntry(
                syncId = sale.id, // Use Sale ID for idempotency
                ownerId = ownerId,
                ownerType = ownerType,
                amount = sale.amount,
                amountGross = sale.amount,
                amountNet = sale.amount,
                type = "revenue",
                category = "MARKETPLACE_SALE",
                notes = "Marketplace Sale: ${sale.listingId}",
                date = sale.completedAt,
                scopeType = "USER",
                scopeId = sale.sellerUserId
            )

            // 4. Record via repository
            financialRepository.addEntry(entry)
            
            Logger.d(TAG, "Successfully synced sale ${sale.id} to finance for $ownerType $ownerId")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to sync sale ${sale.id} to finance", e)
            Result.failure(e)
        }
    }
}
