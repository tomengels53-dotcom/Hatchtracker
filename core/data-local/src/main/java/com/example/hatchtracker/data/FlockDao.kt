package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.FlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlockEntityDao {
    @Query("SELECT * FROM flocks WHERE active = 1 AND deleted = 0 ORDER BY lastUpdated DESC")
    fun getAllActiveFlockEntitys(): Flow<List<FlockEntity>>

    @Query("SELECT * FROM flocks WHERE id = :id")
    suspend fun getFlockEntityById(id: Long): FlockEntity?
    
    @Query("SELECT * FROM flocks WHERE syncId = :syncId LIMIT 1")
    suspend fun getFlockEntityBySyncId(syncId: String): FlockEntity?

    @Query("SELECT * FROM flocks WHERE id = :id")
    fun getFlockEntityFlow(id: Long): Flow<FlockEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlockEntity(flock: FlockEntity): Long

    @Update
    suspend fun updateFlockEntity(flock: FlockEntity): Int

    @Delete
    suspend fun deleteFlockEntity(flock: FlockEntity): Int
    
    // Helper to get bird count for a flock
    @Query("SELECT COUNT(*) FROM birds WHERE flockId = :flockId")
    fun getBirdCountForFlockEntity(flockId: Long): Flow<Int>

    @Query("SELECT DISTINCT breed FROM birds WHERE flockId = :flockId AND status = 'active'")
    suspend fun getDistinctBreedsForFlockEntity(flockId: Long): List<String>

    // Sync Methods
    @Query("SELECT * FROM flocks WHERE cloudId = :cloudId")
    suspend fun getFlockEntityByCloudId(cloudId: String): FlockEntity?

    @Query("SELECT * FROM flocks WHERE pendingSync = 1")
    suspend fun getDirtyFlockEntitys(): List<FlockEntity>

    @Query("UPDATE flocks SET pendingSync = 0, serverUpdatedAt = :serverTimestamp WHERE cloudId = :cloudId")
    suspend fun confirmSync(cloudId: String, serverTimestamp: Long)

    @Transaction
    suspend fun upsertByCloudId(flock: FlockEntity) {
        val existing = getFlockEntityByCloudId(flock.cloudId)
        if (existing != null) {
            updateFlockEntity(flock.copy(id = existing.id))
        } else {
            insertFlockEntity(flock.copy(id = 0))
        }
    }

    @Query("UPDATE flocks SET ownerUserId = :userId WHERE ownerUserId IS NULL")
    suspend fun claimOwnership(userId: String)

    @Query("SELECT * FROM flocks WHERE active = 1 AND deleted = 0")
    suspend fun getAllActiveFlockEntitysOnce(): List<FlockEntity>

    @Query("SELECT COUNT(*) FROM flocks WHERE active = 1 AND deleted = 0")
    suspend fun getActiveFlockEntityCountSync(): Int

    @Query("DELETE FROM flocks WHERE deleted = 1 AND serverUpdatedAt < :cutoff AND pendingSync = 0")
    suspend fun deleteExpiredTombstones(cutoff: Long): Int

    /** Used by DomainEventReplayer.rebuildAll to iterate all non-deleted flocks. */
    @Query("SELECT * FROM flocks WHERE deleted = 0")
    suspend fun getAllFlockEntitysSync(): List<FlockEntity>
}
