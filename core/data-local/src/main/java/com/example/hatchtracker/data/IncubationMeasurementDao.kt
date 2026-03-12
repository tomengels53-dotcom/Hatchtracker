package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hatchtracker.data.models.IncubationMeasurement
import kotlinx.coroutines.flow.Flow

@Dao
interface IncubationMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: IncubationMeasurement): Long

    @Query("SELECT * FROM incubation_measurements WHERE incubationId = :incubationId ORDER BY timestamp DESC")
    fun observeForIncubation(incubationId: Long): Flow<List<IncubationMeasurement>>
    
    @Query("SELECT * FROM incubation_measurements WHERE incubationId = :incubationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(incubationId: Long): IncubationMeasurement?
}
