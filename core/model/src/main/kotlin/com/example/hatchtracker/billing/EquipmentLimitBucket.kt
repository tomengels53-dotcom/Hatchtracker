package com.example.hatchtracker.billing

/**
 * Defines the subscription gating buckets used for equipment inventory limits.
 * This grouping prevents non-essential monitoring devices from blocking essential care or housing equipment.
 */
enum class EquipmentLimitBucket(val label: String) {
    INCUBATION_CORE("Incubation"),
    BROODING("Brooding"),
    HOUSING("Housing"),
    CARE("Flock Care"),
    MONITORING("Monitoring")
}
