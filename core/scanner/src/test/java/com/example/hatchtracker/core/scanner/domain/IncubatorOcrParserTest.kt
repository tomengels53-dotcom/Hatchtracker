package com.example.hatchtracker.core.scanner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncubatorOcrParserTest {

    private val parser = IncubatorOcrParser()

    @Test
    fun `parse - standard incubator screen text`() {
        val rawText = """
            TEMP: 37.5 C
            HUM: 45%
            SET: 37.5
        """.trimIndent()

        val result = parser.parse(rawText)

        assertEquals(37.5, result.temperatureC!!, 0.01)
        assertEquals(45, result.humidityPercent)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `parse - comma as decimal separator`() {
        val rawText = """
            Temp: 37,8
            Humidity: 50
        """.trimIndent()

        val result = parser.parse(rawText)

        assertEquals(37.8, result.temperatureC!!, 0.01)
        // Humidity is 50 because it's in the fallback range
        assertEquals(50, result.humidityPercent)
    }

    @Test
    fun `parse - out of range temperature warning`() {
        val rawText = """
            TEMP: 55.0 C
            HUM: 20%
        """.trimIndent()

        val result = parser.parse(rawText)

        assertEquals(55.0, result.temperatureC!!, 0.01)
        assertTrue(result.warnings.any { it.contains("out of expected incubation range") })
    }

    @Test
    fun `parse - no temperature found`() {
        val rawText = "Just some random text without decimals"
        val result = parser.parse(rawText)

        assertEquals(null, result.temperatureC)
        assertTrue(result.warnings.any { it.contains("Could not confidently identify temperature") })
    }
}
