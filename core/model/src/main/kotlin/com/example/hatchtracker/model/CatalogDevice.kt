package com.example.hatchtracker.model

/**
 * Metadata model for predefined equipment options in the catalog.
 * Distinct from the persisted [Device] entity owned by the user.
 */
data class CatalogDevice(
    val id: String,
    val displayName: String,
    val deviceType: DeviceType,
    val category: DeviceCategory,
    val manufacturer: String? = null,
    val model: String? = null,
    val isGeneric: Boolean = false,
    val capacityEggs: Int = 0,
    val features: DeviceFeatures = DeviceFeatures()
)
