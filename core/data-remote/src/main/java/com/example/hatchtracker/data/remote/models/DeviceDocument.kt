package com.example.hatchtracker.data.remote.models

import com.example.hatchtracker.data.models.DeviceFeatures
import com.example.hatchtracker.data.models.DeviceType
import com.google.firebase.firestore.DocumentId

data class DeviceDocument(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val type: DeviceType = DeviceType.INCUBATOR,
    val modelId: String = "",
    val displayName: String = "",
    val capacityEggs: Int = 0,
    val features: DeviceFeatures = DeviceFeatures(),
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    
    // Phase 4 Lifecycle Metadata (Optional for Firestore compatibility)
    val purchaseDate: Long? = null,
    val purchasePrice: Double? = null,
    val residualValue: Double? = null,
    val lifecycleStatus: String? = null, // Store as String for simplicity in DTO
    val disposedAt: Long? = null
)
