package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hatchtracker.data.models.NotificationHistory

@Dao
interface NotificationHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: NotificationHistory): Long

    @Query("SELECT * FROM notification_history WHERE incubationId = :incubationId AND ruleId = :ruleId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTrigger(incubationId: Long, ruleId: String): NotificationHistory?

    @Query("SELECT EXISTS(SELECT 1 FROM notification_history WHERE eventId = :eventId)")
    suspend fun isEventProcessed(eventId: String): Boolean
    
    @Query("DELETE FROM notification_history WHERE timestamp < :timestamp")
    suspend fun cleanupOldHistory(timestamp: Long): Int
}
