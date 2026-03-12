package com.example.hatchtracker.data.models

data class IncubationProfile(
    val species: String,
    val incubationDurationDays: Int,
    
    val temperature: TemperatureRange,
    val humidity: HumidityProfile,

    val lockdownStartDay: Int,

    val turning: TurningRules,

    val ventilationRequirement: String, // "low", "medium", "high"
    val notes: String? = null
)

data class TemperatureRange(
    val min: Double,
    val optimal: Double,
    val max: Double
)

data class HumidityProfile(
    val earlyPhase: MinMaxRange,
    val lockdownPhase: MinMaxRange
)

data class MinMaxRange(
    val min: Int,
    val max: Int
)

data class TurningRules(
    val required: Boolean,
    val timesPerDay: Int,
    val stopDay: Int
)

