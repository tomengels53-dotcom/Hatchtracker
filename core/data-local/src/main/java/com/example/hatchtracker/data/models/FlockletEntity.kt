package com.example.hatchtracker.data.models

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "flocklets",
    indices = [
        Index(value = ["hatchId"]),
        Index(value = ["movedToFlockId"])
    ]
)
data class FlockletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hatchId: Long? = null,
    val species: String,
    @ColumnInfo(defaultValue = "[]")
    val breeds: List<String> = listOf("Mixed"),
    val hatchDate: Long,
    val chickCount: Int,
    
    val currentTemp: Double,
    var targetTemp: Double,
    var ageInDays: Int = 0,
    var weightAvg: Double = 0.0,
    var healthStatus: String = "Healthy",
    var notes: String? = null,
    
    var readyForFlock: Boolean = false,
    var movedToFlockId: Long? = null,
    
    val syncId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "'FLOCKLET'")
    val lifecycleStage: BirdLifecycleStage = BirdLifecycleStage.FLOCKLET,
    val lastUpdated: Long = System.currentTimeMillis(),

    // Multi-user Support (Phase 5)
    @ColumnInfo(defaultValue = "'USER'")
    val scopeType: String = "USER",
    val scopeId: String? = null,

    // Lifecycle Costing (Option B)
    @ColumnInfo(defaultValue = "0")
    val costBasisCents: Long = 0,
    val costBasisSourceRef: String? = null,
    @ColumnInfo(defaultValue = "1")
    val costBasisSchemaVersion: Int = 1
)
