package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.BirdEntity
import com.example.hatchtracker.data.models.SalesBatch
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesBatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesBatch(batch: SalesBatch): Long

    @Query("SELECT * FROM sales_batches WHERE ownerId = :ownerId ORDER BY saleDate DESC")
    fun getSalesByOwner(ownerId: String): Flow<List<SalesBatch>>

    @Query("SELECT * FROM birds WHERE flockId = (SELECT id FROM flocks WHERE syncId = :flockSyncId) AND status = 'active' ORDER BY hatchDate ASC LIMIT :count")
    suspend fun getOldestActiveBirds(flockSyncId: String, count: Int): List<BirdEntity>

    @Query("SELECT AVG(unitPrice) FROM sales_batches WHERE itemType = :itemType")
    suspend fun getAverageSalePrice(itemType: String): Double?

    @Query("SELECT * FROM sales_batches")
    suspend fun getAllBatches(): List<SalesBatch>
}

