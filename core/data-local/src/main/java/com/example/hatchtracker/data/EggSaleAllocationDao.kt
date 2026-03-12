package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hatchtracker.data.models.EggSaleAllocationEntity

@Dao
interface EggSaleAllocationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllocation(allocation: EggSaleAllocationEntity): Long

    @Query("SELECT * FROM egg_sale_allocation WHERE saleId = :saleId")
    suspend fun getAllocationsBySale(saleId: Long): List<EggSaleAllocationEntity>

    @Query("DELETE FROM egg_sale_allocation WHERE saleId = :saleId")
    suspend fun deleteAllocationsBySale(saleId: Long)

    /**
     * Counts how many eggs from a given production log are allocated to non-cancelled sales.
     * Used by the integrity validator.
     */
    @Query("""
        SELECT COALESCE(SUM(a.allocatedCount), 0)
        FROM egg_sale_allocation a
        INNER JOIN egg_sale s ON s.id = a.saleId
        WHERE a.productionLogId = :productionLogId
          AND s.cancelled = 0
    """)
    suspend fun sumActiveAllocationsForLog(productionLogId: String): Int

    /**
     * Total allocated count for a sale (for integrity checks).
     */
    @Query("SELECT COALESCE(SUM(allocatedCount), 0) FROM egg_sale_allocation WHERE saleId = :saleId")
    suspend fun sumAllocationsForSale(saleId: Long): Int
}
