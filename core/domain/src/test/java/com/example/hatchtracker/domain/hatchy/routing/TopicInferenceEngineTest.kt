package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TopicInferenceEngineTest {

    private lateinit var lexiconRegistry: ILexiconRegistry
    private lateinit var engine: TopicInferenceEngine

    @Before
    fun setup() {
        lexiconRegistry = mockk()
        engine = TopicInferenceEngine(lexiconRegistry)
    }

    @Test
    fun `inferTopics should combine lexicon and question mode signals`() {
        // Given: Lexicon identifies CATEGORY_BREAKDOWN
        every { lexiconRegistry.matchTopics(any()) } returns mapOf(DataTopic.CATEGORY_BREAKDOWN to 1.0)
        
        // When: Mode is USER_DATA_STATUS (which boosts DataTopics)
        val result = engine.inferTopics(
            query = "show breakdown",
            entities = emptyList(),
            questionMode = QuestionModeResult(
                primaryMode = QuestionMode.USER_DATA_STATUS,
                secondaryMode = null,
                modeConfidence = 0.9,
                appAnchorScore = 0.0,
                realWorldAnchorScore = 0.0,
                userDataAnchorScore = 1.0
            )
        )
        
        // Then: Score should include lexical (1.0) + mode bias (0.3)
        assertEquals(1.3, result.topicScores[DataTopic.CATEGORY_BREAKDOWN]!!, 0.01)
        assertEquals(DataTopic.CATEGORY_BREAKDOWN, result.primaryTopic)
    }

    @Test
    fun `inferTopics should boost topics based on entities`() {
        // Given: Lexicon identifies TEMPERATURE
        every { lexiconRegistry.matchTopics(any()) } returns mapOf(KnowledgeTopic.TEMPERATURE to 1.0)
        
        // When: Entity is INCUBATION_TOPIC
        val result = engine.inferTopics(
            query = "temperature",
            entities = listOf(HatchyEntity(EntityType.INCUBATION_TOPIC, "TEMPERATURE", "temp", 1.0)),
            questionMode = QuestionModeResult(
                primaryMode = QuestionMode.REAL_WORLD_GUIDANCE,
                secondaryMode = null,
                modeConfidence = 0.9,
                appAnchorScore = 0.0,
                realWorldAnchorScore = 1.0,
                userDataAnchorScore = 0.0
            )
        )
        
        // Then: KnowledgeTopic should get extra boost (0.2 from entity + 0.3 from mode)
        assertEquals(1.5, result.topicScores[KnowledgeTopic.TEMPERATURE]!!, 0.01)
    }

    @Test
    fun `inferTopics should handle multiple topics and return primary`() {
        // Given: Multiple lexical matches
        every { lexiconRegistry.matchTopics(any()) } returns mapOf(
            KnowledgeTopic.TEMPERATURE to 0.8,
            KnowledgeTopic.HUMIDITY to 0.6
        )
        
        val result = engine.inferTopics(
            query = "temp and humidity",
            entities = emptyList(),
            questionMode = QuestionModeResult(
                primaryMode = QuestionMode.REAL_WORLD_GUIDANCE,
                secondaryMode = null,
                modeConfidence = 0.9,
                appAnchorScore = 0.0,
                realWorldAnchorScore = 1.0,
                userDataAnchorScore = 0.0
            )
        )
        
        // Then: Primary should be the highest one
        assertEquals(KnowledgeTopic.TEMPERATURE, result.primaryTopic)
        assertEquals(KnowledgeTopic.HUMIDITY, result.secondaryTopic)
    }
}
