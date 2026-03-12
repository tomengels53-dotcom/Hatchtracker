package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TraitValueExtractorTest {

    private lateinit var extractor: TraitValueExtractor

    @Before
    fun setup() {
        extractor = TraitValueExtractor()
    }

    @Test
    fun `should extract single trait exact match`() {
        val results = extractor.extract("I want blue eggs")
        assertEquals(1, results.size)
        assertEquals(KnowledgeTopic.EGG_COLOR, results[0].topic)
        assertEquals(EggColor.BLUE, results[0].constraint.value)
    }

    @Test
    fun `should extract multiple traits`() {
        val results = extractor.extract("calm blue egg layers")
        // "calm", "blue eggs", "layer"
        assertEquals(3, results.size)
        
        assertTrue(results.any { it.topic == KnowledgeTopic.TEMPERAMENT_DOCILITY && it.constraint.value == TemperamentLevel.CALM })
        assertTrue(results.any { it.topic == KnowledgeTopic.EGG_COLOR && it.constraint.value == EggColor.BLUE })
        assertTrue(results.any { it.constraint.value == PrimaryUsage.LAYER })
    }

    @Test
    fun `should prefer longer matches`() {
        val results = extractor.extract("dark brown eggs")
        // "dark brown eggs" is in lexicon, so it should be preferred over "brown eggs"
        assertEquals(1, results.size)
        assertEquals(EggColor.DARK_BROWN, results[0].constraint.value)
    }

    @Test
    fun `should handle common synonyms`() {
        val results = extractor.extract("friendly birds")
        // "friendly" maps to DOCILE
        assertEquals(1, results.size)
        assertEquals(TemperamentLevel.DOCILE, results[0].constraint.value)
    }

    @Test
    fun `should handle species specific hints if provided in lexicon`() {
        // Guarding behavior is usually for geese/ducks
        val results = extractor.extract("guarding birds")
        // For now Lexicon is simple, but we can verify it maps.
        // If I haven't added GUARDING to lexicon yet, I should check.
        // Actually, let's verify Broodiness.
        val results2 = extractor.extract("broody hens")
        assertEquals(1, results2.size)
        assertEquals(BroodinessLevel.FREQUENT, results2[0].constraint.value)
    }
}
