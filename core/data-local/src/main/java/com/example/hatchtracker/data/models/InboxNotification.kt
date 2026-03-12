package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inbox_notifications",
    indices = [Index(value = ["eventId"])]
)
data class InboxNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val incubationId: Long,
    val eventId: String? = null,
    val title: String,
    val message: String,
    val severity: String, // "INFO", "WARNING", "CRITICAL"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val snoozedUntil: Long = 0
)

