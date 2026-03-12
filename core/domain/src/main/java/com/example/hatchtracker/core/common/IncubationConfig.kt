package com.example.hatchtracker.core.common

import kotlin.math.roundToInt

enum class IncubationTaskType {
    TURN, COOL, MIST, CANDLE, LOCKDOWN, HATCH_WINDOW, CHECK_PARAMS
}

data class Range(val min: Double, val max: Double, val optimal: Double)

data class IncubationConfig(
    val speciesName: String,
    val incubationDays: Int,
    val temperature: Range,
    val humidityDays1ToLockdown: Range,
    val humidityLockdown: Range,
    val turningUntilDay: Int,
    val coolingStartDay: Int? = null,
    val coolingDurationMinutes: String? = null, // e.g., "10-15"
    val mistingStartDay: Int? = null,
    val ventilationIncreaseDay: Int? = null,
    val lockdownDay: Int,
    val candleDays: List<Int>,
    val hatchWindowStartDay: Int
)

object SpeciesConfigs {
    val CHICKEN = IncubationConfig(
        speciesName = "Chicken",
        incubationDays = 21,
        temperature = Range(36.8, 37.8, 37.5),
        humidityDays1ToLockdown = Range(45.0, 55.0, 50.0),
        humidityLockdown = Range(65.0, 75.0, 70.0),
        turningUntilDay = 18,
        ventilationIncreaseDay = 10,
        lockdownDay = 18,
        candleDays = listOf(7, 14),
        hatchWindowStartDay = 20
    )

    val DUCK = IncubationConfig(
        speciesName = "Duck",
        incubationDays = 28,
        temperature = Range(36.8, 37.8, 37.5),
        humidityDays1ToLockdown = Range(45.0, 55.0, 50.0),
        humidityLockdown = Range(65.0, 75.0, 70.0),
        turningUntilDay = 25,
        coolingStartDay = 10,
        coolingDurationMinutes = "10â€“15",
        mistingStartDay = 10,
        lockdownDay = 25,
        candleDays = listOf(7, 14, 21),
        hatchWindowStartDay = 27
    )

    val GOOSE = IncubationConfig(
        speciesName = "Goose",
        incubationDays = 30, // Using 30 as upper bound base, range 28-30
        temperature = Range(36.6, 37.8, 37.3),
        humidityDays1ToLockdown = Range(45.0, 55.0, 50.0),
        humidityLockdown = Range(70.0, 80.0, 75.0),
        turningUntilDay = 25,
        coolingStartDay = 7,
        coolingDurationMinutes = "15â€“20",
        mistingStartDay = 7,
        lockdownDay = 25,
        candleDays = listOf(10, 17, 24),
        hatchWindowStartDay = 28
    )

    val TURKEY = IncubationConfig(
        speciesName = "Turkey",
        incubationDays = 28,
        temperature = Range(36.8, 37.8, 37.5),
        humidityDays1ToLockdown = Range(50.0, 55.0, 52.5),
        humidityLockdown = Range(65.0, 70.0, 67.5),
        turningUntilDay = 25,
        ventilationIncreaseDay = 14,
        lockdownDay = 25,
        candleDays = listOf(7, 14, 21),
        hatchWindowStartDay = 27
    )

    val PEAFOWL = IncubationConfig(
        speciesName = "Peafowl",
        incubationDays = 29, // Avg of 28-30
        temperature = Range(36.7, 37.8, 37.4),
        humidityDays1ToLockdown = Range(45.0, 55.0, 50.0),
        humidityLockdown = Range(70.0, 75.0, 72.5),
        turningUntilDay = 25,
        ventilationIncreaseDay = 14,
        lockdownDay = 25,
        candleDays = listOf(10, 17, 24),
        hatchWindowStartDay = 28
    )

    fun getForSpecies(speciesName: String): IncubationConfig {
        // Simple matching, could be more robust
        return when {
            speciesName.contains("Duck", ignoreCase = true) -> DUCK
            speciesName.contains("Goose", ignoreCase = true) -> GOOSE
            speciesName.contains("Turkey", ignoreCase = true) -> TURKEY
            speciesName.contains("Peafowl", ignoreCase = true) -> PEAFOWL
            speciesName.contains("Chicken", ignoreCase = true) -> CHICKEN
            else -> CHICKEN // Default fallback
        }
    }
}
