package com.example.hatchtracker.domain.hatchy.routing.services

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for standardizing temperature formatting across Hatchy knowledge services.
 * Supports Celsius, Fahrenheit, and Dual-unit presentation.
 */
@Singleton
class TemperatureFormatter @Inject constructor() {

    enum class UnitMode {
        CELSIUS,
        FAHRENHEIT,
        DUAL_CELSIUS_FIRST, // 35°C (95°F)
        DUAL_FAHRENHEIT_FIRST // 95°F (35°C)
    }

    /**
     * Formats a temperature value. 
     * @param fahrenheit The baseline temperature in Fahrenheit.
     * @param mode The desired output format.
     */
    fun format(fahrenheit: Double, mode: UnitMode = UnitMode.DUAL_CELSIUS_FIRST): String {
        val celsius = fahrenheitToCelsius(fahrenheit)
        
        return when (mode) {
            UnitMode.CELSIUS -> "${formatValue(celsius)}°C"
            UnitMode.FAHRENHEIT -> "${formatValue(fahrenheit)}°F"
            UnitMode.DUAL_CELSIUS_FIRST -> "${formatValue(celsius)}°C (${formatValue(fahrenheit)}°F)"
            UnitMode.DUAL_FAHRENHEIT_FIRST -> "${formatValue(fahrenheit)}°F (${formatValue(celsius)}°C)"
        }
    }

    /**
     * Formats a temperature range.
     */
    fun formatRange(fMin: Double, fMax: Double, mode: UnitMode = UnitMode.DUAL_CELSIUS_FIRST): String {
        val cMin = fahrenheitToCelsius(fMin)
        val cMax = fahrenheitToCelsius(fMax)

        return when (mode) {
            UnitMode.CELSIUS -> "${formatValue(cMin)}-${formatValue(cMax)}°C"
            UnitMode.FAHRENHEIT -> "${formatValue(fMin)}-${formatValue(fMax)}°F"
            UnitMode.DUAL_CELSIUS_FIRST -> {
                "${formatValue(cMin)}-${formatValue(cMax)}°C (${formatValue(fMin)}-${formatValue(fMax)}°F)"
            }
            UnitMode.DUAL_FAHRENHEIT_FIRST -> {
                "${formatValue(fMin)}-${formatValue(fMax)}°F (${formatValue(cMin)}-${formatValue(cMax)}°C)"
            }
        }
    }

    private fun fahrenheitToCelsius(f: Double): Double {
        return (f - 32) * 5 / 9
    }

    private fun formatValue(value: Double): String {
        // Round to 1 decimal place if not a whole number, otherwise show as integer
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            "%.1f".format(Locale.US, value)
        }
    }
}
