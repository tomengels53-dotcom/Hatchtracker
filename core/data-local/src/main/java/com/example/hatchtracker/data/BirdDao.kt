package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.BirdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BirdEntityDao {
    @Query("SELECT * FROM birds WHERE id = :id")
    suspend fun getBirdEntityById(id: Long): BirdEntity?

    @Query("SELECT * FROM birds WHERE id = :id")
    fun getBirdEntityByIdFlow(id: Long): Flow<BirdEntity?>

    @Query("SELECT * FROM birds WHERE status = 'active' ORDER BY species ASC, breed ASC")
    fun getActiveBirdEntitysFlow(): Flow<List<BirdEntity>>

    @Query("SELECT * FROM birds WHERE flockId = :flockId AND status = 'active'")
    fun getBirdEntitysByFlockIdFlow(flockId: Long): Flow<List<BirdEntity>>

    @Query("SELECT * FROM birds WHERE flockId IN (:flockIds) AND status = 'active'")
    fun getBirdEntitysInFlocksFlow(flockIds: List<Long>): Flow<List<BirdEntity>>

    @Query("SELECT * FROM birds ORDER BY species ASC, breed ASC")
    fun getAllBirdEntitysFlow(): Flow<List<BirdEntity>>

    @Query("SELECT * FROM birds")
    suspend fun getAllBirdEntitys(): List<BirdEntity>

    @Query("SELECT * FROM birds WHERE motherId = :motherId")
    suspend fun getBirdEntitysByMotherId(motherId: Long): List<BirdEntity>

    @Query("SELECT * FROM birds WHERE incubationId = :incubationId")
    suspend fun getBirdEntitysByIncubationId(incubationId: Long): List<BirdEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirdEntity(bird: BirdEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirdEntitys(birds: List<BirdEntity>): List<Long>

    @Update
    suspend fun updateBirdEntity(bird: BirdEntity): Int

    @Query("SELECT * FROM birds WHERE cloudId = :cloudId")
    suspend fun getBirdEntityByCloudId(cloudId: String): BirdEntity?

    @Query("SELECT * FROM birds WHERE pendingSync = 1")
    suspend fun getDirtyBirdEntitys(): List<BirdEntity>

    @Query("UPDATE birds SET pendingSync = 0, serverUpdatedAt = :serverTimestamp WHERE cloudId = :cloudId")
    suspend fun confirmSync(cloudId: String, serverTimestamp: Long)

    @Transaction
    suspend fun upsertByCloudId(bird: BirdEntity) {
        val existing = getBirdEntityByCloudId(bird.cloudId)
        if (existing != null) {
            updateBirdEntity(bird.copy(id = existing.id))
        } else {
            insertBirdEntity(bird.copy(id = 0))
        }
    }

    @Query("UPDATE birds SET ownerUserId = :userId WHERE ownerUserId IS NULL")
    suspend fun claimOwnership(userId: String)

    @Query("SELECT COUNT(*) FROM birds WHERE flockId = :flockId AND status = 'active'")
    suspend fun getBirdEntityCountForFlockSync(flockId: Long): Int

    @Query("DELETE FROM birds WHERE deleted = 1 AND serverUpdatedAt < :cutoff AND pendingSync = 0")
    suspend fun deleteExpiredTombstones(cutoff: Long): Int
}
