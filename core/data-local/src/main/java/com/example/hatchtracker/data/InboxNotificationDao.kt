package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hatchtracker.data.models.InboxNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxNotificationDao {
    
    @Query("SELECT * FROM inbox_notifications WHERE snoozedUntil < :now ORDER BY timestamp DESC")
    fun getAllActive(now: Long = System.currentTimeMillis()): Flow<List<InboxNotification>>

    @Query("SELECT * FROM inbox_notifications WHERE incubationId = :incubationId AND snoozedUntil < :now ORDER BY timestamp DESC")
    fun getForIncubation(incubationId: Long, now: Long = System.currentTimeMillis()): Flow<List<InboxNotification>>

    @Query("SELECT COUNT(*) FROM inbox_notifications WHERE isRead = 0 AND snoozedUntil < :now")
    fun getUnreadCount(now: Long = System.currentTimeMillis()): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: InboxNotification): Long

    @Query("UPDATE inbox_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long): Int

    @Query("UPDATE inbox_notifications SET snoozedUntil = :snoozeUntil, isRead = 1 WHERE id = :id")
    suspend fun snooze(id: Long, snoozeUntil: Long): Int
    
    @Query("DELETE FROM inbox_notifications WHERE id = :id")
    suspend fun delete(id: Long): Int
    
    @Query("DELETE FROM inbox_notifications WHERE isRead = 1 AND severity = 'INFO'")
    suspend fun clearReadInfo(): Int
}
