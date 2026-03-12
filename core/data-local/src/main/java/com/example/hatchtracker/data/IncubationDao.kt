package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.IncubationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncubationEntityDao {
    @Query("SELECT * FROM incubations WHERE id = :id")
    suspend fun getIncubationEntityById(id: Long): IncubationEntity?

    @Query("SELECT * FROM incubations WHERE syncId = :syncId")
    suspend fun getIncubationEntityBySyncId(syncId: String): IncubationEntity?

    @Query("SELECT * FROM incubations WHERE id = :id")
    fun getIncubationEntityFlow(id: Long): Flow<IncubationEntity?>

    @Query("SELECT * FROM incubations ORDER BY startDate DESC")
    fun getAllIncubationEntitysFlow(): Flow<List<IncubationEntity>>

    @Query("SELECT * FROM incubations")
    suspend fun getAllIncubationEntitys(): List<IncubationEntity>

    @Query("SELECT COUNT(*) FROM incubations WHERE hatchCompleted = 0")
    suspend fun getActiveIncubationEntityCount(): Int

    @Query("SELECT * FROM incubations WHERE hatchCompleted = 0")
    suspend fun getAllActiveIncubationEntitys(): List<IncubationEntity>

    @Query("SELECT * FROM incubations WHERE birdId = :birdId")
    suspend fun getIncubationEntitysByBirdId(birdId: Long): List<IncubationEntity>

    @Query("SELECT * FROM incubations WHERE fatherBirdId = :fatherId")
    suspend fun getIncubationEntitysByFatherId(fatherId: Long): List<IncubationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncubationEntity(incubation: IncubationEntity): Long

    @Update
    suspend fun updateIncubationEntity(incubation: IncubationEntity): Int

    @Delete
    suspend fun deleteIncubationEntity(incubation: IncubationEntity): Int

    // Sync Methods
    @Query("SELECT * FROM incubations WHERE cloudId = :cloudId")
    suspend fun getIncubationEntityByCloudId(cloudId: String): IncubationEntity?

    @Query("SELECT * FROM incubations WHERE pendingSync = 1")
    suspend fun getDirtyIncubationEntitys(): List<IncubationEntity>

    @Query("UPDATE incubations SET pendingSync = 0, serverUpdatedAt = :serverTimestamp WHERE cloudId = :cloudId")
    suspend fun confirmSync(cloudId: String, serverTimestamp: Long)

    @Transaction
    suspend fun upsertByCloudId(incubation: IncubationEntity) {
        val existing = getIncubationEntityByCloudId(incubation.cloudId)
        if (existing != null) {
            updateIncubationEntity(incubation.copy(id = existing.id))
        } else {
            insertIncubationEntity(incubation.copy(id = 0))
        }
    }
    @Query("UPDATE incubations SET ownerUserId = :userId WHERE ownerUserId IS NULL")
    suspend fun claimOwnership(userId: String)
    @Query("DELETE FROM incubations WHERE deleted = 1 AND serverUpdatedAt < :cutoff AND pendingSync = 0")
    suspend fun deleteExpiredTombstones(cutoff: Long): Int
}


