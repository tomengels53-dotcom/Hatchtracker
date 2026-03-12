package com.example.hatchtracker.core.scanner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class ReceiptParserTest {

    private val parser = ReceiptParser()

    @Test
    fun `parse - simple supermarket receipt`() {
        val rawText = """
            AVEVE VEEVOEDER
            Brusselsesteenweg 123
            TOTAL: 45.50 EUR
            VAT: 2.50
            DATE: 12/05/2026
        """.trimIndent()

        val result = parser.parse(rawText)

        assertEquals("AVEVE VEEVOEDER", result.vendor)
        assertEquals(45.50, result.total!!, 0.01)
        assertEquals("EUR", result.currency)
        
        val cal = Calendar.getInstance()
        cal.timeInMillis = result.dateEpochMillis!!
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH))
        assertEquals(12, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `parse - dollar receipt`() {
        val rawText = """
            PETCO STORE #456
            Amount Due: $12.99
            2026-06-15
        """.trimIndent()

        val result = parser.parse(rawText)

        assertEquals("PETCO STORE #456", result.vendor)
        assertEquals(12.99, result.total!!, 0.01)
        assertEquals("USD", result.currency)
        
        val cal = Calendar.getInstance()
        cal.timeInMillis = result.dateEpochMillis!!
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `parse - fallback to max amount`() {
        val rawText = """
            Farmer's Market
            Eggs: 5.00
            Feed: 25.00
            Supplies: 10.00
        """.trimIndent()

        val result = parser.parse(rawText)

        assertEquals("Farmer's Market", result.vendor)
        assertEquals(25.00, result.total!!, 0.01) // Max value as fallback
    }
}
