package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hatchtracker.data.models.EggProductionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EggProductionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: EggProductionEntity)

    // UPSERT by Natural Key logic handled via Repository or @Transaction if necessary.
    // Since we have a Unique Index on (flockId, lineId, dateEpochDay), simple Insert with REPLACE works 
    // IF the ID matches. If ID doesn't match but Natural Key does, we have a conflict.
    // Room's OnConflictStrategy.REPLACE replaces based on Primary Key usually.
    // To Upsert by Natural Key, we usually read first or use a specific query.
    
    @Query("SELECT * FROM egg_production WHERE flockId = :flockId AND lineId IS :lineId AND dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getByNaturalKey(flockId: String, lineId: String?, dateEpochDay: Long): EggProductionEntity?

    @Query("SELECT * FROM egg_production WHERE flockId = :flockId AND deleted = 0 ORDER BY dateEpochDay DESC")
    fun getDailyHistory(flockId: String): Flow<List<EggProductionEntity>>

    @Query("SELECT * FROM egg_production WHERE flockId = :flockId AND dateEpochDay >= :fromEpochDay AND deleted = 0 ORDER BY dateEpochDay DESC")
    fun observeRecentProduction(flockId: String, fromEpochDay: Long): Flow<List<EggProductionEntity>>

    @Query("""
        SELECT SUM(totalEggs) FROM egg_production 
        WHERE flockId = :flockId 
        AND dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay 
        AND (lineId = :lineId OR :lineId IS NULL)
        AND deleted = 0
    """)
    suspend fun getSumTotalEggs(flockId: String, fromEpochDay: Long, toEpochDay: Long, lineId: String?): Int?

    @Query("""
        SELECT SUM(totalEggs - crackedEggs - setForIncubation - soldEggs) FROM egg_production 
        WHERE flockId = :flockId 
        AND dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay
        AND (lineId = :lineId OR :lineId IS NULL)
        AND deleted = 0
    """)
    suspend fun getSumTableEggs(flockId: String, fromEpochDay: Long, toEpochDay: Long, lineId: String?): Int?

    @Query("""
        SELECT SUM(setForIncubation) FROM egg_production 
        WHERE flockId = :flockId 
        AND dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay 
        AND (lineId = :lineId OR :lineId IS NULL)
        AND deleted = 0
    """)
    suspend fun getSumSetForIncubation(flockId: String, fromEpochDay: Long, toEpochDay: Long, lineId: String?): Int?
    
    @Query("SELECT * FROM egg_production WHERE id = :id")
    suspend fun getById(id: String): EggProductionEntity?

    @Query("SELECT * FROM egg_production WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getByCloudId(cloudId: String): EggProductionEntity?

    @Query("UPDATE egg_production SET syncState = 'SYNCED', updatedAt = :serverTimestamp WHERE cloudId = :cloudId")
    suspend fun confirmSync(cloudId: String, serverTimestamp: Long)

    // Breed Line Support
    @Query("SELECT * FROM breed_lines WHERE flockId = :flockId AND deleted = 0 ORDER BY label ASC")
    fun getBreedLines(flockId: String): Flow<List<com.example.hatchtracker.data.models.BreedLineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreedLine(line: com.example.hatchtracker.data.models.BreedLineEntity)

    @Query("SELECT * FROM breed_lines WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getBreedLineByCloudId(cloudId: String): com.example.hatchtracker.data.models.BreedLineEntity?

    @Query("UPDATE breed_lines SET syncState = 'SYNCED', updatedAt = :serverTimestamp WHERE cloudId = :cloudId")
    suspend fun confirmBreedLineSync(cloudId: String, serverTimestamp: Long)

    @Query("""
        SELECT * FROM egg_production 
        WHERE flockId = :flockId 
        AND (totalEggs - crackedEggs - setForIncubation - soldEggs) > 0 
        AND deleted = 0 
        ORDER BY dateEpochDay ASC
    """)
    suspend fun getAvailableLogsForFlock(flockId: String): List<EggProductionEntity>

    /** Same as getAvailableLogsForFlock but explicitly ASC for FIFO sale allocation. */
    @Query("""
        SELECT * FROM egg_production 
        WHERE flockId = :flockId 
        AND (totalEggs - crackedEggs - setForIncubation - soldEggs) > 0 
        AND deleted = 0 
        ORDER BY dateEpochDay ASC
    """)
    suspend fun getAvailableLogsForFlockSaleFIFO(flockId: String): List<EggProductionEntity>

    @Query("SELECT * FROM egg_production WHERE flockId = :flockId AND setForIncubation > 0 AND deleted = 0 ORDER BY dateEpochDay DESC")
    suspend fun getReservedLogsForFlock(flockId: String): List<EggProductionEntity>

    @Query("UPDATE egg_production SET setForIncubation = setForIncubation + :delta, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementSetForIncubation(id: String, delta: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE egg_production SET setForIncubation = setForIncubation - :delta, updatedAt = :timestamp WHERE id = :id")
    suspend fun decrementSetForIncubation(id: String, delta: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE egg_production SET soldEggs = soldEggs + :delta, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementSoldEggs(id: String, delta: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE egg_production SET soldEggs = MAX(0, soldEggs - :delta), updatedAt = :timestamp WHERE id = :id")
    suspend fun decrementSoldEggs(id: String, delta: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Sums (totalEggs - crackedEggs) for cost basis derivation.
     * Does NOT subtract reserved or sold — cost basis uses produced eggs only.
     */
    @Query("""
        SELECT SUM(totalEggs - crackedEggs) FROM egg_production
        WHERE flockId = :flockId
        AND dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay
        AND deleted = 0
    """)
    suspend fun sumNetProducedInWindow(flockId: String, fromEpochDay: Long, toEpochDay: Long): Int?

    @Query("SELECT * FROM egg_production WHERE deleted = 0")
    suspend fun getAllLogsSync(): List<EggProductionEntity>

    @Query("""
        UPDATE egg_production 
        SET setForIncubation = (
            SELECT COALESCE(SUM(reservedCount), 0) 
            FROM egg_reservation 
            WHERE productionLogId = egg_production.id
        ) 
        WHERE flockId = :flockId
    """)
    suspend fun rebuildSetForIncubationForFlock(flockId: String)

    /**
     * Rebuilds soldEggs from active (non-cancelled) sale allocations.
     * Must be called AFTER rebuildSetForIncubationForFlock.
     */
    @Query("""
        UPDATE egg_production 
        SET soldEggs = (
            SELECT COALESCE(SUM(a.allocatedCount), 0)
            FROM egg_sale_allocation a
            INNER JOIN egg_sale s ON s.id = a.saleId
            WHERE a.productionLogId = egg_production.id
              AND s.cancelled = 0
        )
        WHERE flockId = :flockId
    """)
    suspend fun rebuildSoldEggsForFlock(flockId: String)
}
