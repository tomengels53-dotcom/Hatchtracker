package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TopicCoverageTest(private val case: TopicTestCase) {

    companion object {
        data class TopicTestCase(
            val query: String,
            val expectedTopic: HatchyTopic,
            val expectedQuestionMode: QuestionMode
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(TopicTestCase("how long is the incubation period", KnowledgeTopic.INCUBATION_PERIOD, QuestionMode.UNKNOWN)),
            arrayOf(TopicTestCase("how is my batch doing", DataTopic.ACTIVE_BATCH_STATUS, QuestionMode.USER_DATA_STATUS)),
            arrayOf(TopicTestCase("what breeds should i cross", KnowledgeTopic.CROSSBREED_RECOMMENDATION, QuestionMode.REAL_WORLD_GUIDANCE)),
            arrayOf(TopicTestCase("where in the app do i start a new incubation", WorkflowTopic.START_INCUBATION, QuestionMode.APP_WORKFLOW))
        )
    }

    @Test
    fun verifyRoutingSignals() {
        val topicEngine = TopicInferenceEngine(LexiconRegistry())
        val modeClassifier = QuestionModeClassifier()
        val breedRepo = mockk<IBreedStandardRepository>(relaxed = true)
        val entityExtractor = HatchyEntityExtractor(breedRepo)

        val mode = modeClassifier.classify(case.query)
        val entities = entityExtractor.extract(case.query)
        val topicResult = topicEngine.inferTopics(case.query, entities, mode)

        assertEquals("Query: ${case.query} -> Wrong Topic", case.expectedTopic, topicResult.primaryTopic)
        assertEquals("Query: ${case.query} -> Wrong Question Mode", case.expectedQuestionMode, mode.primaryMode)
    }
}
