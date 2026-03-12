package com.example.hatchtracker.domain.hatchy.routing

import org.junit.Assert.assertEquals
import org.junit.Test

class LexiconNormalizationTest {

    @Test
    fun `normalize should handle casing and punctuation`() {
        val input = "How long is the INCUBATION period???"
        val expected = "how long is the incubation period"
        assertEquals(expected, HatchyNormalization.normalize(input))
    }

    @Test
    fun `normalize should expand common abbreviations`() {
        val input = "what is the temp for chicks"
        val expected = "what is the temperature for chick"
        assertEquals(expected, HatchyNormalization.normalize(input))
    }

    @Test
    fun `normalize should singularize common poultry terms`() {
        val input = "when do eggs hatch"
        val expected = "when do egg hatch"
        assertEquals(expected, HatchyNormalization.normalize(input))
        
        val input2 = "care for baby chicks"
        val expected2 = "care for baby chick"
        assertEquals(expected2, HatchyNormalization.normalize(input2))
    }

    @Test
    fun `normalize should handle shorthand like F2`() {
        val input = "variation in f2 generation"
        val expected = "variation in 2nd generation generation"
        assertEquals(expected, HatchyNormalization.normalize(input))
    }
}
