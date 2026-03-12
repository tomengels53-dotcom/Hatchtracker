package com.example.hatchtracker.data.catalog

import com.example.hatchtracker.model.CatalogDevice
import com.example.hatchtracker.model.DeviceFeatures
import com.example.hatchtracker.model.DeviceType
import com.example.hatchtracker.model.DeviceCategory

/**
 * Static source of truth for Official Equipment Models.
 * Normalized to use CatalogDevice for metadata separation.
 */
object DeviceCatalog {

    val OFFICIAL_MODELS = listOf(
        // --- BRINSEA ---
        CatalogDevice(
            id = "brinsea-mini-ii-adv",
            displayName = "Brinsea Mini II Advance",
            manufacturer = "Brinsea",
            model = "Mini II Advance",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 7,
            features = DeviceFeatures(
                autoTurn = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "brinsea-maxi-24-adv",
            displayName = "Brinsea Maxi 24 Advance",
            manufacturer = "Brinsea",
            model = "Maxi 24 Advance",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 24,
            features = DeviceFeatures(
                autoTurn = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "brinsea-ovation-28-eco",
            displayName = "Brinsea Ovation 28 Eco",
            manufacturer = "Brinsea",
            model = "Ovation 28 Eco",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 28,
            features = DeviceFeatures(
                autoTurn = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "brinsea-ovation-28-adv",
            displayName = "Brinsea Ovation 28 Advance",
            manufacturer = "Brinsea",
            model = "Ovation 28 Advance",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 28,
            features = DeviceFeatures(
                autoTurn = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "brinsea-ovation-56-ex",
            displayName = "Brinsea Ovation 56 EX",
            manufacturer = "Brinsea",
            model = "Ovation 56 EX",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 56,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "brinsea-mini-ii-ex",
            displayName = "Brinsea Mini II EX",
            manufacturer = "Brinsea",
            model = "Mini II EX",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 7,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),

        // --- RCOM ---
        CatalogDevice(
            id = "rcom-pro-10",
            displayName = "Rcom Pro 10",
            manufacturer = "Rcom",
            model = "Pro 10",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 10,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "rcom-20-max",
            displayName = "Rcom 20 Max",
            manufacturer = "Rcom",
            model = "20 Max",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 20,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "rcom-20-pro",
            displayName = "Rcom 20 Pro",
            manufacturer = "Rcom",
            model = "20 Pro",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 20,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "rcom-king-suro-20",
            displayName = "Rcom King Suro 20",
            manufacturer = "Rcom",
            model = "King Suro 20",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 24,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "rcom-50-max",
            displayName = "Rcom 50 MAX",
            manufacturer = "Rcom",
            model = "50 MAX",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 48,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "rcom-maru-190",
            displayName = "Rcom Maru 190 Deluxe",
            manufacturer = "Rcom",
            model = "Maru 190 Deluxe",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 168,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),

        // --- HEKA ---
        CatalogDevice(
            id = "heka-1-70",
            displayName = "HEKA 1",
            manufacturer = "HEKA",
            model = "1 (70 Eggs)",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 70,
            features = DeviceFeatures(
                autoTurn = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),
        CatalogDevice(
            id = "heka-euro-lux-1",
            displayName = "HEKA Euro-Lux 1",
            manufacturer = "HEKA",
            model = "Euro-Lux 1",
            deviceType = DeviceType.SETTER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 90,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = true,
                autoTemperature = true,
                requiresManualLockdown = true,
                supportsExternalHatcher = true
            )
        ),

        // --- HATCHERS ---
        CatalogDevice(
            id = "brinsea-ovation-56-eco-hatcher",
            displayName = "Brinsea Ovation 56 Eco Hatcher",
            manufacturer = "Brinsea",
            model = "Ovation 56 Eco Hatcher",
            deviceType = DeviceType.HATCHER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 56,
            features = DeviceFeatures(autoTurn = false, autoTemperature = true)
        ),
        CatalogDevice(
            id = "rcom-maru-hatcher",
            displayName = "Rcom Maru Hatcher",
            manufacturer = "Rcom",
            model = "Maru Hatcher",
            deviceType = DeviceType.HATCHER,
            category = DeviceCategory.INCUBATION,
            capacityEggs = 100,
            features = DeviceFeatures(autoTurn = false, autoHumidity = true, autoTemperature = true)
        ),

        // --- BROODING ---
        CatalogDevice(
            id = "generic-brood-plate",
            displayName = "Generic Brood Plate",
            deviceType = DeviceType.BROOD_PLATE,
            category = DeviceCategory.BROODING,
            isGeneric = true
        ),
        CatalogDevice(
            id = "generic-heat-lamp",
            displayName = "Generic Heat Lamp",
            deviceType = DeviceType.HEAT_LAMP,
            category = DeviceCategory.BROODING,
            isGeneric = true
        ),

        // --- HOUSING ---
        CatalogDevice(
            id = "generic-coop",
            displayName = "Generic Coop",
            deviceType = DeviceType.COOP,
            category = DeviceCategory.HOUSING,
            isGeneric = true
        ),
        CatalogDevice(
            id = "generic-nest-box",
            displayName = "Generic Nest Box",
            deviceType = DeviceType.NEST_BOX,
            category = DeviceCategory.HOUSING,
            isGeneric = true
        ),

        // --- CARE ---
        CatalogDevice(
            id = "generic-feeder",
            displayName = "Generic Feeder",
            deviceType = DeviceType.FEEDER,
            category = DeviceCategory.CARE,
            isGeneric = true
        ),
        CatalogDevice(
            id = "generic-waterer",
            displayName = "Generic Waterer",
            deviceType = DeviceType.WATERER,
            category = DeviceCategory.CARE,
            isGeneric = true
        ),

        // --- MONITORING ---
        CatalogDevice(
            id = "generic-candler",
            displayName = "Generic Candler",
            deviceType = DeviceType.CANDLER,
            category = DeviceCategory.MONITORING,
            isGeneric = true
        ),
        CatalogDevice(
            id = "generic-hygrometer",
            displayName = "Generic Hygrometer",
            deviceType = DeviceType.HYGROMETER,
            category = DeviceCategory.MONITORING,
            isGeneric = true
        ),
        CatalogDevice(
            id = "generic-camera",
            displayName = "Generic WiFi Camera",
            deviceType = DeviceType.CAMERA,
            category = DeviceCategory.MONITORING,
            isGeneric = true
        )
    )

    fun getAllModels(): List<CatalogDevice> = OFFICIAL_MODELS
    
    fun getModel(modelId: String): CatalogDevice? = OFFICIAL_MODELS.find { it.id == modelId }

    @Deprecated("Use getAllModels returning CatalogDevice instead")
    fun getIncubatorModels(): List<CatalogDevice> = OFFICIAL_MODELS.filter { it.deviceType == DeviceType.SETTER }

    @Deprecated("Use getAllModels returning CatalogDevice instead")
    fun getHatcherModels(): List<CatalogDevice> = OFFICIAL_MODELS.filter { it.deviceType == DeviceType.HATCHER }
}

