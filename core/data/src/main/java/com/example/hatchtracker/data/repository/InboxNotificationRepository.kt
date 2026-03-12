package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.InboxNotificationDao
import com.example.hatchtracker.data.models.InboxNotification
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxNotificationRepository @Inject constructor(
    private val inboxNotificationDao: InboxNotificationDao
) {

    fun getAllActive(): Flow<List<InboxNotification>> {
        return inboxNotificationDao.getAllActive()
    }

    fun getForIncubation(incubationId: Long): Flow<List<InboxNotification>> {
        return inboxNotificationDao.getForIncubation(incubationId)
    }

    fun getUnreadCount(): Flow<Int> {
        return inboxNotificationDao.getUnreadCount()
    }

    suspend fun insert(notification: InboxNotification): Long {
        return inboxNotificationDao.insert(notification)
    }

    suspend fun markAsRead(id: Long): Int {
        return inboxNotificationDao.markAsRead(id)
    }

    suspend fun snooze(id: Long, snoozeUntil: Long): Int {
        return inboxNotificationDao.snooze(id, snoozeUntil)
    }

    suspend fun delete(id: Long): Int {
        return inboxNotificationDao.delete(id)
    }

    suspend fun clearReadInfo(): Int {
        return inboxNotificationDao.clearReadInfo()
    }
}
