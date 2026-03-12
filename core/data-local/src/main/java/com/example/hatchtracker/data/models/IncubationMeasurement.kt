package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incubation_measurements", 
    indices = [Index("incubationId")]
)
data class IncubationMeasurement(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val incubationId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Double? = null,
    val humidityPercent: Int? = null,
    val source: String = "OCR", // "MANUAL" or "OCR"
    val rawText: String? = null
)
