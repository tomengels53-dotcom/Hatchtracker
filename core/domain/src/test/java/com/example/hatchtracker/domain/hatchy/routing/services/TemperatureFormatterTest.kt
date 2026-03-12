package com.example.hatchtracker.domain.hatchy.routing.services

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureFormatterTest {

    private val formatter = TemperatureFormatter()

    @Test
    fun `format should handle Celsius mode`() {
        val result = formatter.format(95.0, TemperatureFormatter.UnitMode.CELSIUS)
        assertEquals("35°C", result)
    }

    @Test
    fun `format should handle Fahrenheit mode`() {
        val result = formatter.format(35.0, TemperatureFormatter.UnitMode.FAHRENHEIT) // Passing 35F for test
        assertEquals("35°F", result)
    }

    @Test
    fun `format should handle Dual Celsius First mode`() {
        val result = formatter.format(99.5, TemperatureFormatter.UnitMode.DUAL_CELSIUS_FIRST)
        assertEquals("37.5°C (99.5°F)", result)
    }

    @Test
    fun `format should handle Dual Fahrenheit First mode`() {
        val result = formatter.format(99.5, TemperatureFormatter.UnitMode.DUAL_FAHRENHEIT_FIRST)
        assertEquals("99.5°F (37.5°C)", result)
    }

    @Test
    fun `formatRange should handle Dual Celsius First mode`() {
        val result = formatter.formatRange(95.0, 99.5, TemperatureFormatter.UnitMode.DUAL_CELSIUS_FIRST)
        assertEquals("35-37.5°C (95-99.5°F)", result)
    }
}
