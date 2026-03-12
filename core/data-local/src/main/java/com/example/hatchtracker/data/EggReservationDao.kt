package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.EggReservationEntity

@Dao
interface EggReservationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReservation(res: EggReservationEntity): Long

    @Query("SELECT * FROM egg_reservation WHERE incubationId = :incubationId")
    suspend fun getReservationsByIncubation(incubationId: Long): List<EggReservationEntity>

    @Query("DELETE FROM egg_reservation WHERE incubationId = :incubationId")
    suspend fun deleteReservationsByIncubation(incubationId: Long): Int

    @Query("SELECT SUM(reservedCount) FROM egg_reservation WHERE productionLogId = :productionLogId")
    suspend fun getReservedSumForLog(productionLogId: String): Int?

    @Query("SELECT * FROM egg_reservation WHERE productionLogId = :productionLogId")
    suspend fun getReservationsByLog(productionLogId: String): List<EggReservationEntity>

    @Query("SELECT productionLogId AS logId, SUM(reservedCount) AS totalReserved FROM egg_reservation GROUP BY productionLogId")
    suspend fun summarizeAllReservationsByLog(): List<ReservationSummary>

    @Query("SELECT incubationId AS incubationId, SUM(reservedCount) AS totalReserved FROM egg_reservation GROUP BY incubationId")
    suspend fun summarizeAllReservationsByIncubation(): List<IncubationSummary>
}

data class ReservationSummary(val logId: String, val totalReserved: Int)
data class IncubationSummary(val incubationId: Long, val totalReserved: Int)
