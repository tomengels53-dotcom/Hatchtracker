package com.example.hatchtracker.domain.hatchy.routing

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class QuestionModeClassifierTest {

    private lateinit var classifier: QuestionModeClassifier

    @Before
    fun setup() {
        classifier = QuestionModeClassifier()
    }

    @Test
    fun `classify should identify REAL_WORLD_GUIDANCE`() {
        val query = "How to set up a new incubator?"
        val result = classifier.classify(query)
        assertEquals(QuestionMode.REAL_WORLD_GUIDANCE, result.primaryMode)
    }

    @Test
    fun `classify should identify APP_WORKFLOW`() {
        val query = "Where in the app do I start a new incubation?"
        val result = classifier.classify(query)
        assertEquals(QuestionMode.APP_WORKFLOW, result.primaryMode)
    }

    @Test
    fun `classify should identify USER_DATA_STATUS`() {
        val query = "Which incubation is closest to hatch?"
        val result = classifier.classify(query)
        assertEquals(QuestionMode.USER_DATA_STATUS, result.primaryMode)
    }

    @Test
    fun `classify should identify MIXED mode`() {
        // "app" and "save" -> APP, "how to" and "set up" -> REAL_WORLD.
        val query = "How to set up a new incubator and save it in the app?"
        val result = classifier.classify(query)
        assertEquals(QuestionMode.MIXED, result.primaryMode)
        // Ensure either REAL_WORLD or APP_WORKFLOW is the secondary indicating the competition
        assert(result.secondaryMode == QuestionMode.REAL_WORLD_GUIDANCE || result.secondaryMode == QuestionMode.APP_WORKFLOW)
    }

    @Test
    fun `classify should identify UNKNOWN below threshold`() {
        val query = "Tell me something"
        val result = classifier.classify(query)
        assertEquals(QuestionMode.UNKNOWN, result.primaryMode)
        assertEquals(0.5, result.modeConfidence, 0.01)
    }
}
