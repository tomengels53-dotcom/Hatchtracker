package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.BreedingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BreedingDao {
    @Query("SELECT * FROM breeding_records WHERE id = :id")
    suspend fun getBreedingRecordById(id: Long): BreedingRecord?

    @Query("SELECT * FROM breeding_records ORDER BY dateStarted DESC")
    fun getAllBreedingRecordsFlow(): Flow<List<BreedingRecord>>

    @Query("SELECT * FROM breeding_records WHERE status = 'active'")
    fun getActiveBreedingRecordsFlow(): Flow<List<BreedingRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreedingRecord(record: BreedingRecord): Long

    @Update
    suspend fun updateBreedingRecord(record: BreedingRecord): Int

    @Delete
    suspend fun deleteBreedingRecord(record: BreedingRecord): Int
}
