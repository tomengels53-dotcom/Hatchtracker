package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.FinancialEntry
import com.example.hatchtracker.data.models.FinancialSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {
    // Append-only for entries
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: FinancialEntry): Long

    @Delete
    suspend fun deleteEntry(entry: FinancialEntry)
    
    @Query("SELECT * FROM financial_entries WHERE ownerId = :ownerId AND ownerType = :ownerType ORDER BY date DESC")
    suspend fun getEntriesForOwnerSync(ownerId: String, ownerType: String): List<FinancialEntry>

    @Query("SELECT * FROM financial_entries WHERE ownerId = :ownerId AND ownerType = :ownerType ORDER BY date DESC")
    fun getEntriesForOwnerFlow(ownerId: String, ownerType: String): Flow<List<FinancialEntry>>

    @Query("SELECT * FROM financial_entries WHERE ownerType = :ownerType AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getEntriesForTypeInDateRange(ownerType: String, startDate: Long, endDate: Long): Flow<List<FinancialEntry>>

    @Query("SELECT * FROM financial_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getAllEntriesInDateRange(startDate: Long, endDate: Long): Flow<List<FinancialEntry>>
    
    @Query("""
        SELECT * FROM financial_entries 
        WHERE (:ownerType IS NULL OR ownerType = :ownerType)
        AND (:ownerId IS NULL OR ownerId = :ownerId)
        AND (date >= :startDate AND date <= :endDate)
        ORDER BY date ASC
    """)
    fun getFilteredEntries(
        ownerType: String?, 
        ownerId: String?, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<FinancialEntry>>
    
    @Query("SELECT * FROM financial_entries WHERE syncId = :syncId")
    suspend fun getEntryBySyncId(syncId: String): FinancialEntry?

    @Query("SELECT * FROM financial_entries WHERE ownerType = :ownerType AND type = 'cost'")
    suspend fun getCostsForOwnerType(ownerType: String): List<FinancialEntry>

    @Query("""
        SELECT SUM(amountGross) FROM financial_entries 
        WHERE ownerId = :ownerId AND ownerType = :ownerType 
        AND type = 'cost' 
        AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getSumCostsForOwnerInRange(ownerId: String, ownerType: String, startDate: Long, endDate: Long): Double?

    // Summaries are updated from server or locally recalculated
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSummary(summary: FinancialSummaryEntity)
    
    @Query("SELECT * FROM financial_summaries WHERE ownerId = :ownerId AND ownerType = :ownerType")
    fun getSummaryForOwnerFlow(ownerId: String, ownerType: String): Flow<FinancialSummaryEntity?>
    
    @Query("SELECT * FROM financial_summaries WHERE summaryId = :summaryId")
    suspend fun getSummaryById(summaryId: String): FinancialSummaryEntity?

    @Query("SELECT * FROM financial_entries")
    suspend fun getAllEntriesSync(): List<FinancialEntry>

    @Query("SELECT * FROM financial_summaries")
    suspend fun getAllSummariesSync(): List<FinancialSummaryEntity>

    @Query("SELECT * FROM financial_entries WHERE isRecurring = 1")
    suspend fun getRecurringEntries(): List<FinancialEntry>

    // Cost Basis Ledger support
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCostBasisEntry(entry: com.example.hatchtracker.data.models.CostBasisLedgerEntryEntity)

    @Query("SELECT * FROM cost_basis_ledger WHERE entityId = :entityId AND entityType = :entityType")
    suspend fun getCostBasisEntriesForEntity(entityId: String, entityType: String): List<com.example.hatchtracker.data.models.CostBasisLedgerEntryEntity>

    /**
     * Sums flock costs within a millis window (type='cost', ownerType='flock').
     * Uses amountGross with fallback to amount.
     */
    @Query("""
        SELECT COALESCE(SUM(COALESCE(amountGross, amount)), 0.0) FROM financial_entries
        WHERE ownerId = :flockId
          AND ownerType = 'flock'
          AND type = 'cost'
          AND date >= :fromMillis
          AND date <= :toMillis
    """)
    suspend fun getFlockCostSumInWindow(flockId: String, fromMillis: Long, toMillis: Long): Double?

    // Performance Optimized Aggregations (Phase 1 Refined)
    
    @Query("""
        SELECT COALESCE(SUM(CAST(COALESCE(amountGross, amount) * 100 AS INTEGER)), 0) 
        FROM financial_entries
        WHERE (:ownerType IS NULL OR ownerType = :ownerType)
        AND (:ownerId IS NULL OR ownerId = :ownerId)
        AND (date >= :startDate AND date <= :endDate)
        AND type = :type
        AND category != 'hatch_value' -- Exclude snapshot entries from profit
    """)
    fun getSumCents(
        ownerType: String?,
        ownerId: String?,
        startDate: Long,
        endDate: Long,
        type: String
    ): Flow<Long>

    @Query("""
        SELECT COUNT(*) FROM financial_entries
        WHERE (:ownerType IS NULL OR ownerType = :ownerType)
        AND (:ownerId IS NULL OR ownerId = :ownerId)
        AND (date >= :startDate AND date <= :endDate)
    """)
    fun getEntryCount(
        ownerType: String?,
        ownerId: String?,
        startDate: Long,
        endDate: Long
    ): Flow<Int>
}
