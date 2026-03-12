package com.example.hatchtracker.core.common

import com.example.hatchtracker.data.models.*

object IncubationRegistry {

    private val profiles = mapOf(
        "Chicken" to IncubationProfile(
            species = "Chicken",
            incubationDurationDays = 21,
            temperature = TemperatureRange(min = 37.0, optimal = 37.5, max = 38.2),
            humidity = HumidityProfile(
                earlyPhase = MinMaxRange(min = 45, max = 55),
                lockdownPhase = MinMaxRange(min = 65, max = 75)
            ),
            lockdownStartDay = 18,
            turning = TurningRules(required = true, timesPerDay = 5, stopDay = 18),
            ventilationRequirement = "medium"
        ),
        "Duck" to IncubationProfile(
            species = "Duck",
            incubationDurationDays = 28,
            temperature = TemperatureRange(min = 37.0, optimal = 37.5, max = 37.8),
            humidity = HumidityProfile(
                earlyPhase = MinMaxRange(min = 50, max = 60),
                lockdownPhase = MinMaxRange(min = 75, max = 85)
            ),
            lockdownStartDay = 25,
            turning = TurningRules(required = true, timesPerDay = 4, stopDay = 25),
            ventilationRequirement = "high",
            notes = "Muscovy ducks require 35 days."
        ),
        "Goose" to IncubationProfile(
            species = "Goose",
            incubationDurationDays = 30,
            temperature = TemperatureRange(min = 37.0, optimal = 37.2, max = 37.5),
            humidity = HumidityProfile(
                earlyPhase = MinMaxRange(min = 50, max = 60),
                lockdownPhase = MinMaxRange(min = 75, max = 85)
            ),
            lockdownStartDay = 27,
            turning = TurningRules(required = true, timesPerDay = 4, stopDay = 27),
            ventilationRequirement = "high"
        ),
        "Turkey" to IncubationProfile(
            species = "Turkey",
            incubationDurationDays = 28,
            temperature = TemperatureRange(min = 37.0, optimal = 37.5, max = 38.0),
            humidity = HumidityProfile(
                earlyPhase = MinMaxRange(min = 50, max = 55),
                lockdownPhase = MinMaxRange(min = 70, max = 75)
            ),
            lockdownStartDay = 25,
            turning = TurningRules(required = true, timesPerDay = 5, stopDay = 25),
            ventilationRequirement = "medium"
        ),
        "Peafowl" to IncubationProfile(
            species = "Peafowl",
            incubationDurationDays = 28,
            temperature = TemperatureRange(min = 37.0, optimal = 37.5, max = 37.8),
            humidity = HumidityProfile(
                earlyPhase = MinMaxRange(min = 50, max = 55),
                lockdownPhase = MinMaxRange(min = 70, max = 75)
            ),
            lockdownStartDay = 25,
            turning = TurningRules(required = true, timesPerDay = 5, stopDay = 25),
            ventilationRequirement = "medium"
        )
    )

    /**
     * Single source of truth for incubation biology.
     */
    fun getProfileForSpecies(speciesName: String?): IncubationProfile {
        return profiles[speciesName] ?: profiles["Chicken"]!! // Default to Chicken if unknown
    }
}
