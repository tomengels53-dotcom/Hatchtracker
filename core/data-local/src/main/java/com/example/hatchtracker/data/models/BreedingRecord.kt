package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "breeding_records")
data class BreedingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val flockId: Long,
    val species: String,
    val sireId: Long,
    val damIds: List<Long>,
    val dateStarted: Long = System.currentTimeMillis(),
    val status: String = "active", // active, completed, cancelled
    val goals: List<String> = emptyList(),
    val notes: String? = null,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis()
)

