package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hatchtracker.data.models.EggSaleEntity

@Dao
interface EggSaleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(sale: EggSaleEntity): Long

    @Query("SELECT * FROM egg_sale WHERE id = :id")
    suspend fun getSale(id: Long): EggSaleEntity?

    @Query("SELECT * FROM egg_sale WHERE syncId = :syncId")
    suspend fun getSaleBySyncId(syncId: String): EggSaleEntity?

    @Query("SELECT * FROM egg_sale WHERE flockId = :flockId AND cancelled = 0 ORDER BY saleDateEpochDay DESC")
    suspend fun getActiveSalesForFlock(flockId: String): List<EggSaleEntity>

    @Query("SELECT * FROM egg_sale WHERE flockId = :flockId ORDER BY saleDateEpochDay DESC LIMIT :limit OFFSET :offset")
    suspend fun getSalesForFlockPaged(flockId: String, limit: Int, offset: Int): List<EggSaleEntity>

    @Query("UPDATE egg_sale SET cancelled = 1 WHERE id = :id")
    suspend fun markCancelled(id: Long)

    @Query("UPDATE egg_sale SET syncTime = :serverTime WHERE syncId = :syncId")
    suspend fun markSynced(syncId: String, serverTime: Long)

    @Query("DELETE FROM egg_sale WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("SELECT * FROM egg_sale WHERE cancelled = 0")
    suspend fun getAllActiveSales(): List<EggSaleEntity>

    @Query("SELECT * FROM egg_sale")
    suspend fun getAllSales(): List<EggSaleEntity>
}
