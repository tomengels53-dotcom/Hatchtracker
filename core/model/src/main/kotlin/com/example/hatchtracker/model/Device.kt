package com.example.hatchtracker.model

import com.example.hatchtracker.billing.EquipmentLimitBucket

/**
 * Represents a physical piece of equipment or device owned by the user.
 * 
 * Includes incubators, brooders, feeders, cameras, etc.
 */
data class Device(
    val id: String = "",
    val userId: String = "",
    
    val type: DeviceType = DeviceType.SETTER,
    
    // Link to official catalog
    val modelId: String = "",
    
    // User-friendly name (e.g. "My Brinsea")
    val displayName: String = "",
    
    // Hard capacity limit (if applicable, e.g. egg capacity)
    val capacityEggs: Int = 0,
    
    // Derived features (cached from catalog for offline access/speed)
    val features: DeviceFeatures = DeviceFeatures(),
    
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    
    // Phase 4 Lifecycle Metadata
    val purchaseDate: Long? = null,
    val purchasePrice: Double? = null,
    val residualValue: Double? = null,
    val lifecycleStatus: DeviceLifecycleStatus = DeviceLifecycleStatus.ACTIVE,
    val disposedAt: Long? = null
)

enum class DeviceLifecycleStatus {
    ACTIVE,
    DISPOSED
}

data class DeviceFeatures(
    val autoTurn: Boolean = false,
    val autoHumidity: Boolean = false,
    val autoTemperature: Boolean = false,
    val requiresManualLockdown: Boolean = true,
    val supportsExternalHatcher: Boolean = false
)

/**
 * Broad visual and functional groupings for equipment.
 * Used for UI filtering and icon sets.
 */
enum class DeviceCategory {
    INCUBATION,
    BROODING,
    HOUSING,
    CARE,
    MONITORING
}

/**
 * Specific equipment types with exhaustive mapping to categories and subscription buckets.
 * This ensures compile-time safety and prevents gating logic drift.
 */
enum class DeviceType(
    val category: DeviceCategory,
    val bucket: EquipmentLimitBucket
) {
    // --- INCUBATION ---
    SETTER(DeviceCategory.INCUBATION, EquipmentLimitBucket.INCUBATION_CORE),
    HATCHER(DeviceCategory.INCUBATION, EquipmentLimitBucket.INCUBATION_CORE),
    TURNER(DeviceCategory.INCUBATION, EquipmentLimitBucket.INCUBATION_CORE),
    HUMIDITY_CONTROLLER(DeviceCategory.INCUBATION, EquipmentLimitBucket.INCUBATION_CORE),
    THERMOSTAT(DeviceCategory.INCUBATION, EquipmentLimitBucket.INCUBATION_CORE),

    // --- BROODING ---
    BROOD_PLATE(DeviceCategory.BROODING, EquipmentLimitBucket.BROODING),
    HEAT_LAMP(DeviceCategory.BROODING, EquipmentLimitBucket.BROODING),
    HEAT_PANEL(DeviceCategory.BROODING, EquipmentLimitBucket.BROODING),

    // --- HOUSING ---
    COOP(DeviceCategory.HOUSING, EquipmentLimitBucket.HOUSING),
    NEST_BOX(DeviceCategory.HOUSING, EquipmentLimitBucket.HOUSING),
    RUN(DeviceCategory.HOUSING, EquipmentLimitBucket.HOUSING),
    COOP_AUTO(DeviceCategory.HOUSING, EquipmentLimitBucket.HOUSING),

    // --- CARE ---
    FEEDER(DeviceCategory.CARE, EquipmentLimitBucket.CARE),
    WATERER(DeviceCategory.CARE, EquipmentLimitBucket.CARE),
    WASHER(DeviceCategory.CARE, EquipmentLimitBucket.CARE),
    SANITIZER(DeviceCategory.CARE, EquipmentLimitBucket.CARE),

    // --- MONITORING ---
    CANDLER(DeviceCategory.MONITORING, EquipmentLimitBucket.MONITORING),
    SCALE(DeviceCategory.MONITORING, EquipmentLimitBucket.MONITORING),
    THERMOMETER(DeviceCategory.MONITORING, EquipmentLimitBucket.MONITORING),
    HYGROMETER(DeviceCategory.MONITORING, EquipmentLimitBucket.MONITORING),
    CAMERA(DeviceCategory.MONITORING, EquipmentLimitBucket.MONITORING),

    // --- LEGACY (For Firestore Deserialization Safety) ---
    @Deprecated("Use SETTER instead")
    INCUBATOR(DeviceCategory.INCUBATION, EquipmentLimitBucket.INCUBATION_CORE),
    @Deprecated("Use BROOD_PLATE instead")
    BROODER(DeviceCategory.BROODING, EquipmentLimitBucket.BROODING);

    companion object {
        // Compatibility bridge for legacy migrations or unknown types
        fun fromLegacy(name: String?): DeviceType {
            return values().find { it.name.equals(name, ignoreCase = true) } ?: SETTER
        }

        @Deprecated("Use SETTER instead", ReplaceWith("SETTER"))
        val INCUBATOR = SETTER
    }
}

