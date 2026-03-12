package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.hatchtracker.data.models.SyncQueueEntity
import com.example.hatchtracker.data.models.SyncQueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(entry: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY nextRetryAt ASC LIMIT :limit")
    suspend fun getPendingSyncs(status: SyncQueueStatus, limit: Int): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET status = :status, errorMessage = :error, attemptCount = attemptCount + 1, lastAttemptAt = :now, nextRetryAt = :nextRetry WHERE id = :id")
    suspend fun markFailed(id: Long, status: SyncQueueStatus, error: String?, now: Long, nextRetry: Long)

    @Query("DELETE FROM sync_queue WHERE eventId = :eventId")
    suspend fun removeByEventId(eventId: String)

    @Update
    suspend fun update(entry: SyncQueueEntity)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED'")
    fun countPendingSyncs(): Flow<Int>
}
