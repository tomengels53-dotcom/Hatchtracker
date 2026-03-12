package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_history",
    indices = [Index(value = ["eventId"])]
)
data class NotificationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val incubationId: Long,
    val ruleId: String, // Unique identifier for the rule (e.g., "lockdown_start", "temp_high", "turn_reminder")
    val eventId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

