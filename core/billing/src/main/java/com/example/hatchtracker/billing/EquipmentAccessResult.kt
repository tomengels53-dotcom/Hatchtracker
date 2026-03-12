package com.example.hatchtracker.billing

/**
 * Result of an equipment gating evaluation.
 */
data class EquipmentAccessResult(
    val allowed: Boolean,
    val bucket: EquipmentLimitBucket,
    val currentCount: Int,
    val maxAllowed: Int?, // null = unlimited
    val formattedMessage: String? = null
)
