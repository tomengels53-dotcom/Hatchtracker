package com.example.hatchtracker.domain.breeding.quant

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

class QuantitativeTraitEngineTest {

    private val engine = QuantitativeTraitEngine()

    @Test
    fun `predictTrait should return deterministic result for same seed`() {
        val seed = 12345L
        val result1 = engine.predictTrait(
            traitId = "TEST",
            sireValue = 5.0,
            damValue = 5.0,
            heritability = 0.5,
            inbreedingCoefficient = 0.0,
            rngSeed = seed
        )

        val result2 = engine.predictTrait(
            traitId = "TEST",
            sireValue = 5.0,
            damValue = 5.0,
            heritability = 0.5,
            inbreedingCoefficient = 0.0,
            rngSeed = seed
        )

        assertEquals("Results should be identical for same seed", result1, result2, 0.0001)
    }

    @Test
    fun `predictTrait should return different results for different seeds`() {
        val seed1 = 12345L
        val seed2 = 67890L
        
        val result1 = engine.predictTrait(
            traitId = "TEST",
            sireValue = 5.0,
            damValue = 5.0,
            heritability = 0.5,
            inbreedingCoefficient = 0.0,
            rngSeed = seed1
        )

        val result2 = engine.predictTrait(
            traitId = "TEST",
            sireValue = 5.0,
            damValue = 5.0,
            heritability = 0.5,
            inbreedingCoefficient = 0.0,
            rngSeed = seed2
        )

        assertNotEquals("Results should differ for different seeds (with high probability)", result1, result2, 0.0001)
    }
}
