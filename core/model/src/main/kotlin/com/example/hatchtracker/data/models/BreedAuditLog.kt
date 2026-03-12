package com.example.hatchtracker.data.models

data class BreedAuditLog(
    val id: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val breedId: String = "",
    val breedName: String = "",
    val action: String = "UPDATE", // CREATE, UPDATE, DELETE, DEPRECATE
    val previousState: Map<String, Any?> = emptyMap(),
    val newState: Map<String, Any?> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

