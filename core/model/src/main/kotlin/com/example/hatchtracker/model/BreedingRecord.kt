package com.example.hatchtracker.model

data class BreedingRecord(
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
