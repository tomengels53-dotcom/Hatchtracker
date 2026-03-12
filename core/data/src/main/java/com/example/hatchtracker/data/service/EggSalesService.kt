package com.example.hatchtracker.data.service

import androidx.room.withTransaction
import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.DomainEventLogger
import com.example.hatchtracker.data.models.EggSaleAllocationEntity
import com.example.hatchtracker.data.models.EggSaleEntity
import com.example.hatchtracker.data.models.FinancialEntry
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates egg sales: FIFO lot allocation, flock-derived COGS, transactional sale/cancel.
 *
 * Design invariants:
 * - All mutations are wrapped in db.withTransaction.
 * - No cross-repository dependency: EggProductionRepository and FinancialRepository are accessed
 *   here via their individual DAOs, keeping repos cleanly decoupled.
 * - Finance entries use INSERT OR IGNORE (syncId UNIQUE) for idempotency.
 * - Cancelled sales keep allocation rows as permanent audit trail; only soldEggs is decremented.
 */
@Singleton
class EggSalesService @Inject constructor(
    private val db: AppDatabase,
    private val domainEventLogger: DomainEventLogger
) {

    companion object {
        /** Rolling cost window in days (inclusive). */
        const val COST_WINDOW_DAYS = 30
    }

    /**
     * Records an egg sale with FIFO allocation from production logs.
     *
     * Steps (all inside one transaction):
     * 1. Validate inputs
     * 2. Derive COGS from rolling 30-day flock cost window
     * 3. Verify eggs are available
     * 4. Insert EggSaleEntity
     * 5. FIFO allocate from production log snapshot
     * 6. Insert revenue + COGS finance entries (idempotent)
     * 7. Log domain events
     *
     * @param flockId String UUID of the flock
     * @param saleDateEpochDay Date of sale as LocalDate.toEpochDay()
     * @param quantity Eggs sold (must be > 0)
     * @param pricePerEggCents Price per egg in cents (>= 0)
     * @param notes Optional notes
     * @return inserted EggSaleEntity
     */
    suspend fun sellHatchingEggs(
        flockId: String,
        saleDateEpochDay: Long,
        quantity: Int,
        pricePerEggCents: Long,
        notes: String? = null
    ): EggSaleEntity {
        require(quantity > 0) { "quantity must be > 0" }
        require(pricePerEggCents >= 0) { "pricePerEggCents must be >= 0" }
        require(saleDateEpochDay <= LocalDate.now().toEpochDay()) { "sale date cannot be in the future" }

        return db.withTransaction {
            val eggProdDao = db.eggProductionDao()
            val eggSaleDao = db.eggSaleDao()
            val eggSaleAllocDao = db.eggSaleAllocationDao()
            val finDao = db.financialDao()

            // --- 1. Derive COGS (30-day rolling window) ---
            val fromEpochDay = saleDateEpochDay - (COST_WINDOW_DAYS - 1)
            // Convert epoch days to milliseconds for existing finance date field
            val fromMillis = LocalDate.ofEpochDay(fromEpochDay).atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            val toMillis = LocalDate.ofEpochDay(saleDateEpochDay).atTime(23, 59, 59)
                .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()

            val costSumDouble = finDao.getFlockCostSumInWindow(flockId, fromMillis, toMillis) ?: 0.0
            // Convert to cents (multiply by 100, round half-up)
            val costSumCents = BigDecimal(costSumDouble)
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()

            val netEggsInWindow = eggProdDao.sumNetProducedInWindow(flockId, fromEpochDay, saleDateEpochDay) ?: 0
            val noEggsInWindow = (netEggsInWindow == 0)

            val costPerEggCents = BigDecimal(costSumCents)
                .divide(BigDecimal(maxOf(netEggsInWindow, 1)), 0, RoundingMode.HALF_UP)
                .toLong()
            val totalCogsCents = costPerEggCents * quantity
            val totalRevenueCents = pricePerEggCents * quantity

            // --- 2. Verify availability (snapshot inside transaction) ---
            val logs = eggProdDao.getAvailableLogsForFlockSaleFIFO(flockId)
            val totalAvailable = logs.sumOf {
                maxOf(0, it.totalEggs - it.crackedEggs - it.setForIncubation - it.soldEggs)
            }
            if (totalAvailable < quantity) {
                throw InsufficientEggsException(available = totalAvailable, requested = quantity)
            }

            // --- 3. Insert sale record ---
            val sale = EggSaleEntity(
                flockId = flockId,
                quantity = quantity,
                saleDateEpochDay = saleDateEpochDay,
                pricePerEggCents = pricePerEggCents,
                totalRevenueCents = totalRevenueCents,
                derivedCostPerEggCents = costPerEggCents,
                totalCogsCents = totalCogsCents,
                notes = notes,
                createdAtEpochMillis = System.currentTimeMillis()
            )
            val saleId = eggSaleDao.insertSale(sale)

            // --- 4. FIFO allocation (in-memory snapshot, no re-fetch) ---
            var remaining = quantity
            for (log in logs) {
                if (remaining <= 0) break
                val avail = maxOf(0, log.totalEggs - log.crackedEggs - log.setForIncubation - log.soldEggs)
                val take = minOf(avail, remaining)
                if (take <= 0) continue
                eggSaleAllocDao.insertAllocation(
                    EggSaleAllocationEntity(saleId = saleId, productionLogId = log.id, allocatedCount = take)
                )
                eggProdDao.incrementSoldEggs(log.id, take)
                remaining -= take
            }
            check(remaining == 0) { "FIFO allocation invariant violated — this should not happen" }

            // --- 5. Finance entries (idempotent via INSERT OR IGNORE on syncId) ---
            val saleDate = LocalDate.ofEpochDay(saleDateEpochDay)
                .atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            finDao.insertEntry(
                FinancialEntry(
                    syncId = "sale_${saleId}_rev",
                    ownerId = flockId,
                    ownerType = "flock",
                    type = "revenue",
                    category = "Egg Sale",
                    amount = totalRevenueCents / 100.0,
                    amountGross = totalRevenueCents / 100.0,
                    amountNet = totalRevenueCents / 100.0,
                    date = saleDate,
                    notes = "Egg sale #$saleId — $quantity eggs @ ${pricePerEggCents}c each${notes?.let { ": $it" } ?: ""}"
                )
            )
            finDao.insertEntry(
                FinancialEntry(
                    syncId = "sale_${saleId}_cogs",
                    ownerId = flockId,
                    ownerType = "flock",
                    type = "cost",
                    category = "COGS — Egg Sale",
                    amount = totalCogsCents / 100.0,
                    amountGross = totalCogsCents / 100.0,
                    amountNet = totalCogsCents / 100.0,
                    date = saleDate,
                    notes = "COGS for sale #$saleId — ${costPerEggCents}c/egg, ${COST_WINDOW_DAYS}-day window"
                )
            )

            // --- 6. Domain events ---
            val warningFragment = if (noEggsInWindow) ""","costBasis":"NO_EGGS_IN_WINDOW"""" else ""
            val payload = """{
                "qty": $quantity,
                "revenue": $totalRevenueCents,
                "cogs": $totalCogsCents,
                "costPerEgg": $costPerEggCents,
                "windowDays": $COST_WINDOW_DAYS$warningFragment
            }""".trimIndent()

            domainEventLogger.log("EGG_SALE", sale.syncId, "EGG_SALE_CREATED", payload)

            sale.copy(id = saleId)
        }
    }

    /**
     * Cancels a sale by:
     * - Decrementing soldEggs for all allocated logs
     * - Inserting reversal finance entries (audit trail preserved)
     * - Marking sale as cancelled
     * - Logging EGG_SALE_CANCELLED event
     *
     * Idempotent: calling twice for the same saleId is a no-op.
     */
    suspend fun cancelEggSale(saleId: Long) {
        db.withTransaction {
            val eggSaleDao = db.eggSaleDao()
            val eggSaleAllocDao = db.eggSaleAllocationDao()
            val eggProdDao = db.eggProductionDao()
            val finDao = db.financialDao()

            val sale = eggSaleDao.getSale(saleId) ?: return@withTransaction
            if (sale.cancelled) return@withTransaction  // idempotent

            // Restore soldEggs for each allocation
            val allocs = eggSaleAllocDao.getAllocationsBySale(saleId)
            for (alloc in allocs) {
                eggProdDao.decrementSoldEggs(alloc.productionLogId, alloc.allocatedCount)
            }

            // Reversal finance entries (INSERT OR IGNORE for idempotency)
            val now = System.currentTimeMillis()
            finDao.insertEntry(
                FinancialEntry(
                    syncId = "sale_${saleId}_rev_reversal",
                    ownerId = sale.flockId,
                    ownerType = "flock",
                    type = "revenue",
                    category = "Egg Sale Reversal",
                    amount = -sale.totalRevenueCents / 100.0,
                    amountGross = -sale.totalRevenueCents / 100.0,
                    date = now,
                    notes = "Reversal for cancelled sale #$saleId"
                )
            )
            finDao.insertEntry(
                FinancialEntry(
                    syncId = "sale_${saleId}_cogs_reversal",
                    ownerId = sale.flockId,
                    ownerType = "flock",
                    type = "cost",
                    category = "COGS Reversal",
                    amount = -sale.totalCogsCents / 100.0,
                    amountGross = -sale.totalCogsCents / 100.0,
                    date = now,
                    notes = "COGS reversal for cancelled sale #$saleId"
                )
            )

            eggSaleDao.markCancelled(saleId)

            domainEventLogger.log(
                "EGG_SALE",
                sale.syncId,
                "EGG_SALE_CANCELLED",
                """{"saleId": $saleId, "reversedQty": ${sale.quantity}}"""
            )
        }
    }
}

/** Thrown when requested egg quantity exceeds available supply. */
class InsufficientEggsException(val available: Int, val requested: Int) :
    IllegalStateException("Cannot sell $requested eggs: only $available available")
