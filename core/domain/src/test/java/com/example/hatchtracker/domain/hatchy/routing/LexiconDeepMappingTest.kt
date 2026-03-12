package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LexiconDeepMappingTest {

    private lateinit var registry: LexiconRegistry

    @Before
    fun setup() {
        registry = LexiconRegistry()
    }

    @Test
    fun `lockdown maps semantically to LOCKDOWN knowledge topic`() {
        val results = registry.matchTopics("lockdown")
        assertTrue("Should contain LOCKDOWN topic", results.containsKey(KnowledgeTopic.LOCKDOWN))
    }

    @Test
    fun `stop turning maps semantically to LOCKDOWN knowledge topic`() {
        val results = registry.matchTopics("stop turning")
        assertTrue("Should contain LOCKDOWN topic", results.containsKey(KnowledgeTopic.LOCKDOWN))
    }

    @Test
    fun `moisture maps semantically to HUMIDITY knowledge topic`() {
        val results = registry.matchTopics("moisture")
        assertTrue("Should contain HUMIDITY topic", results.containsKey(KnowledgeTopic.HUMIDITY))
        // Verify it doesn't accidentally map to Temperature (correcting previous ambiguity)
        assertTrue("Should NOT contain TEMPERATURE as primary for moisture", (results[KnowledgeTopic.TEMPERATURE] ?: 0.0) < (results[KnowledgeTopic.HUMIDITY] ?: 0.0))
    }

    @Test
    fun `humidity maps semantically to HUMIDITY knowledge topic`() {
        val results = registry.matchTopics("humidity")
        assertTrue("Should contain HUMIDITY topic", results.containsKey(KnowledgeTopic.HUMIDITY))
    }

    @Test
    fun `how long to hatch maps to INCUBATION_PERIOD knowledge topic`() {
        val results = registry.matchTopics("how long to hatch")
        assertTrue("Should contain INCUBATION_PERIOD topic", results.containsKey(KnowledgeTopic.INCUBATION_PERIOD))
    }

    @Test
    fun `temperature maps to both incubation and brooder temperature`() {
        val results = registry.matchTopics("temperature")
        assertTrue("Should contain incubation TEMPERATURE", results.containsKey(KnowledgeTopic.TEMPERATURE))
        assertTrue("Should contain BROODER_TEMPERATURE", results.containsKey(KnowledgeTopic.BROODER_TEMPERATURE))
    }

    @Test
    fun `brooder heat specifically maps to BROODER_TEMPERATURE`() {
        val results = registry.matchTopics("brooder heat")
        assertTrue("Should contain BROODER_TEMPERATURE", results.containsKey(KnowledgeTopic.BROODER_TEMPERATURE))
    }

    @Test
    fun `new incubation maps to START_INCUBATION workflow topic`() {
        val results = registry.matchTopics("new incubation")
        assertTrue("Should contain START_INCUBATION workflow", results.containsKey(WorkflowTopic.START_INCUBATION))
    }

    @Test
    fun `how is my batch doing maps to ACTIVE_BATCH_STATUS data topic`() {
        val results = registry.matchTopics("how is my batch doing")
        assertTrue("Should contain ACTIVE_BATCH_STATUS", results.containsKey(DataTopic.ACTIVE_BATCH_STATUS))
    }

    @Test
    fun `inherit maps to TRAIT_INHERITANCE knowledge topic`() {
        val results = registry.matchTopics("inherit")
        assertTrue("Should contain TRAIT_INHERITANCE", results.containsKey(KnowledgeTopic.TRAIT_INHERITANCE))
    }
}
