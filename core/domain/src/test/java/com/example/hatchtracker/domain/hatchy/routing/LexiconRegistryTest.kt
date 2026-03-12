package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LexiconRegistryTest {

    private lateinit var registry: LexiconRegistry

    @Before
    fun setup() {
        registry = LexiconRegistry()
    }

    @Test
    fun `normalize should handle case and punctuation`() {
        val input = "How long to hatch eggs?"
        val expected = "how long to hatch egg"
        assertEquals(expected, registry.normalize(input))
    }

    @Test
    fun `matchTopics should find exact matches`() {
        val matches = registry.matchTopics("temperature")
        assertTrue(matches.containsKey(KnowledgeTopic.TEMPERATURE))
        assertEquals(1.0, matches[KnowledgeTopic.TEMPERATURE]!!, 0.01)
    }

    @Test
    fun `matchTopics should find partial matches with lower weight`() {
        // "how long to hatch" partially matches the lexicon phrase "how long".
        val matches = registry.matchTopics("how long to hatch")
        assertTrue(matches.containsKey(KnowledgeTopic.INCUBATION_PERIOD))
        // Raw substring matches are weighted lower before normalization.
        assertEquals(1.0, matches[KnowledgeTopic.INCUBATION_PERIOD]!!, 0.01)
    }

    @Test
    fun `matchGoals should find breeding goals`() {
        val goals = registry.matchGoals("I want more eggs")
        assertTrue(goals.contains(BreedingGoal.EGG_PRODUCTION))
    }

    @Test
    fun `matchAnchors should find navigation anchors`() {
        val anchors = registry.matchAnchors("show me settings")
        assertTrue(anchors.containsKey("NAV_SETTINGS"))
        assertEquals(1.0, anchors["NAV_SETTINGS"]!!, 0.01)
    }
}
