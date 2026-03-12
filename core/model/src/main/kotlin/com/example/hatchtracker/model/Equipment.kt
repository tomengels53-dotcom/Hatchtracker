package com.example.hatchtracker.model

/**
 * Semantic type alias for [Device] to transition the domain terminology
 * from "Devices" to "Equipment" while maintaining 100% backward compatibility
 * with the underlying Firestore schema and serialization.
 */
typealias Equipment = Device

/**
 * Semantic type alias for [DeviceType] to align with [Equipment] terminology.
 */
typealias EquipmentType = DeviceType

/**
 * Semantic type alias for [DeviceFeatures] to align with [Equipment] terminology.
 */
typealias EquipmentFeatures = DeviceFeatures
