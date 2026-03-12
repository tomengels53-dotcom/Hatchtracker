package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.FlockletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlockletDao {
    @Query("SELECT * FROM flocklets WHERE movedToFlockId IS NULL ORDER BY hatchDate DESC")
    fun getActiveFlockletsFlow(): Flow<List<FlockletEntity>>

    @Query("SELECT * FROM flocklets WHERE movedToFlockId IS NULL")
    suspend fun getActiveFlockletsSync(): List<FlockletEntity>

    @Query("SELECT * FROM flocklets WHERE hatchId = :hatchId")
    fun getFlockletsByHatchId(hatchId: Long): Flow<List<FlockletEntity>>

    @Query("SELECT * FROM flocklets WHERE hatchId = :hatchId")
    suspend fun getFlockletsByHatchIdSync(hatchId: Long): List<FlockletEntity>

    @Query("SELECT * FROM flocklets WHERE hatchId = :hatchId LIMIT 1")
    suspend fun getFlockletByHatchId(hatchId: Long): FlockletEntity?

    @Query("SELECT * FROM flocklets WHERE id = :id")
    suspend fun getFlockletById(id: Long): FlockletEntity?
    
    @Query("SELECT * FROM flocklets WHERE syncId = :syncId LIMIT 1")
    suspend fun getFlockletBySyncId(syncId: String): FlockletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlocklet(flocklet: FlockletEntity): Long

    @Update
    suspend fun updateFlocklet(flocklet: FlockletEntity): Int

    @Delete
    suspend fun deleteFlocklet(flocklet: FlockletEntity): Int
    
    @Query("SELECT * FROM flocklets WHERE movedToFlockId IS NOT NULL")
    suspend fun getArchivedFlocklets(): List<FlockletEntity>
}
