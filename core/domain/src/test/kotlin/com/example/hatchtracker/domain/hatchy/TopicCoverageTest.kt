package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.routing.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TopicCoverageTest {

    private lateinit var topicEngine: TopicInferenceEngine

    @Before
    fun setup() {
        topicEngine = TopicInferenceEngine(LexiconRegistry())
    }

    @Test
    fun `test breeding domain coverage`() {
        val cases = mapOf(
            "best cross breeds" to KnowledgeTopic.CROSSBREED_RECOMMENDATION,
            "inheritance of traits" to KnowledgeTopic.TRAIT_INHERITANCE,
            "f2 generation issues" to KnowledgeTopic.GENERATION_VARIATION,
            "breeding strategy advice" to KnowledgeTopic.BREEDING_STRATEGY,
            "how to pair for meat" to KnowledgeTopic.GOAL_BASED_PAIRING
        )

        cases.forEach { (query, expected) ->
            val result = topicEngine.inferTopics(query, emptyList(), realWorldMode())
            assertEquals("Topic mismatch for: $query", expected, result.primaryTopic)
            assertTrue("Low confidence for: $query", result.confidence >= 0.6)
        }
    }

    @Test
    fun `test incubation domain coverage`() {
        val cases = mapOf(
            "incubation temperature" to KnowledgeTopic.TEMPERATURE,
            "ideal humidity" to KnowledgeTopic.HUMIDITY,
            "rotate eggs" to KnowledgeTopic.TURNING,
            "lockdown start" to KnowledgeTopic.LOCKDOWN,
            "when do my eggs hatch" to KnowledgeTopic.HATCH_TIMING,
            "install incubator" to KnowledgeTopic.SETUP_DEVICE
        )

        cases.forEach { (query, expected) ->
            val result = topicEngine.inferTopics(query, emptyList(), realWorldMode())
            assertEquals("Topic mismatch for: $query", expected, result.primaryTopic)
            assertTrue("Low confidence for: $query", result.confidence >= 0.6)
        }
    }

    @Test
    fun `test nursery domain coverage`() {
        val cases = mapOf(
            "brooder heat" to KnowledgeTopic.BROODER_TEMPERATURE,
            "when move chicks to coop" to KnowledgeTopic.READY_TO_MOVE
        )

        cases.forEach { (query, expected) ->
            val result = topicEngine.inferTopics(query, emptyList(), realWorldMode())
            assertEquals("Topic mismatch for: $query", expected, result.primaryTopic)
        }
    }

    @Test
    fun `test equipment domain coverage`() {
        val cases = mapOf(
            "calibrate sensor" to KnowledgeTopic.CALIBRATE_DEVICE,
            "clean incubator" to KnowledgeTopic.CLEAN_DEVICE,
            "broken motor repair" to KnowledgeTopic.MAINTENANCE_DUE
        )

        cases.forEach { (query, expected) ->
            val result = topicEngine.inferTopics(query, emptyList(), realWorldMode())
            assertEquals("Topic mismatch for: $query", expected, result.primaryTopic)
        }
    }

    @Test
    fun `test workflow and data coverage`() {
        val cases = mapOf(
            "start a new hatch" to WorkflowTopic.START_INCUBATION,
            "log feed cost" to WorkflowTopic.LOG_EXPENSE,
            "record chick hatch" to WorkflowTopic.RECORD_HATCH,
            "how is my batch doing" to DataTopic.ACTIVE_BATCH_STATUS,
            "how many died" to DataTopic.LOSSES_SUMMARY,
            "total spending summary" to DataTopic.TOTAL_SPEND
        )

        cases.forEach { (query, expected) ->
            val result = topicEngine.inferTopics(query, emptyList(), modeFor(expected))
            assertEquals("Topic mismatch for: $query", expected, result.primaryTopic)
        }
    }

    private fun realWorldMode() = QuestionModeResult(
        primaryMode = QuestionMode.REAL_WORLD_GUIDANCE,
        secondaryMode = null,
        modeConfidence = 1.0,
        appAnchorScore = 0.0,
        realWorldAnchorScore = 1.0,
        userDataAnchorScore = 0.0
    )

    private fun appWorkflowMode() = QuestionModeResult(
        primaryMode = QuestionMode.APP_WORKFLOW,
        secondaryMode = null,
        modeConfidence = 1.0,
        appAnchorScore = 1.0,
        realWorldAnchorScore = 0.0,
        userDataAnchorScore = 0.0
    )

    private fun userDataMode() = QuestionModeResult(
        primaryMode = QuestionMode.USER_DATA_STATUS,
        secondaryMode = null,
        modeConfidence = 1.0,
        appAnchorScore = 0.0,
        realWorldAnchorScore = 0.0,
        userDataAnchorScore = 1.0
    )

    private fun modeFor(expected: HatchyTopic) = when (expected) {
        is WorkflowTopic -> appWorkflowMode()
        is DataTopic -> userDataMode()
        else -> realWorldMode()
    }
}
