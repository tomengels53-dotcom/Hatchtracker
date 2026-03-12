package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "breed_lines",
    indices = [
        Index(value = ["flockId"]),
        Index(value = ["cloudId"], unique = true) // Sync requirement
    ]
)
data class BreedLineEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // Using UUID string for primary key as per modern standards
    
    val flockId: String,
    val label: String,
    
    // Sync Metadata
    val cloudId: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
    val syncState: SyncState = SyncState.PENDING
)
