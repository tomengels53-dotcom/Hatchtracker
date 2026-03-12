package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transactional queue for action-based sync retry logic.
 * Refined for B2 to track DomainEvent eventId instead of state snapshots.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["nextRetryAt"]),
        Index(value = ["status"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Reference to DomainEventEntity.eventId */
    val eventId: String,
    
    val status: SyncQueueStatus = SyncQueueStatus.PENDING,
    
    val attemptCount: Int = 0,
    
    val lastAttemptAt: Long? = null,
    
    val nextRetryAt: Long? = null,
    
    val errorMessage: String? = null
)

enum class SyncQueueStatus {
    PENDING,
    SENT,
    FAILED
}
