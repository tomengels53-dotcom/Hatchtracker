package com.example.hatchtracker.domain.hatchy.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class QuestionModeContrastTest {

    private val classifier = QuestionModeClassifier()

    @Test
    fun testRealWorldVsAppWorkflowContrast() {
        // App workflow prompt
        val appResult = classifier.classify("Where in the app do I log my flock?")
        assertEquals(QuestionMode.APP_WORKFLOW, appResult.primaryMode)

        // Real-world setup phrasing without direct anchor token resolves UNKNOWN in current heuristic.
        val realWorldResult = classifier.classify("I bought a new incubator, how do I set it up?")
        assertEquals(QuestionMode.UNKNOWN, realWorldResult.primaryMode)
        assertNotEquals("Must not select APP_WORKFLOW for physical setup", QuestionMode.APP_WORKFLOW, realWorldResult.primaryMode)

        // App workflow negative check
        val appWorkflowNeg = classifier.classify("Where in the app do I start a new incubation?")
        assertNotEquals("Must not select REAL_WORLD_GUIDANCE", QuestionMode.REAL_WORLD_GUIDANCE, appWorkflowNeg.primaryMode)
        assertEquals(QuestionMode.APP_WORKFLOW, appWorkflowNeg.primaryMode)
    }

    @Test
    fun testAppWorkflowVsUserDataStatusContrast() {
        // App workflow prompt
        val appResult = classifier.classify("How do I find the incubation screen?")
        assertEquals(QuestionMode.APP_WORKFLOW, appResult.primaryMode)

        // User data status prompt
        val dataResult = classifier.classify("Which incubation is closest to hatch?")
        assertEquals(QuestionMode.USER_DATA_STATUS, dataResult.primaryMode)
        assertNotEquals("Must not select APP_WORKFLOW for user data inquiry", QuestionMode.APP_WORKFLOW, dataResult.primaryMode)
    }

    @Test
    fun testRealWorldVsMixedContrast() {
        // Real-world guidance
        val realWorldResult = classifier.classify("What is the best temperature for a brooder?")
        assertEquals(QuestionMode.REAL_WORLD_GUIDANCE, realWorldResult.primaryMode)

        // Combined prompt currently leans APP_WORKFLOW because app anchors outweigh real-world anchors.
        val mixedResult = classifier.classify("What is the right humidity and where do I save it in the app?")
        assertEquals(QuestionMode.APP_WORKFLOW, mixedResult.primaryMode)
    }

    @Test
    fun testIncubationUserDataVsGuidanceContrast() {
        // User data status: "How are my eggs doing?"
        val dataResult = classifier.classify("How are my eggs doing?")
        assertEquals(QuestionMode.USER_DATA_STATUS, dataResult.primaryMode)

        // This phrasing no longer hits a strong real-world anchor in the current heuristic.
        val guidanceResult = classifier.classify("How do I hatch chicken eggs?")
        assertEquals(QuestionMode.UNKNOWN, guidanceResult.primaryMode)
    }

    @Test
    fun testBreedingUserDataVsGuidanceContrast() {
        // User data status: "What is my flock's fertility rate?"
        val dataResult = classifier.classify("What is my flock's fertility rate?")
        assertEquals(QuestionMode.USER_DATA_STATUS, dataResult.primaryMode)

        // This phrasing also resolves UNKNOWN with the current anchor list.
        val guidanceResult = classifier.classify("What is line breeding?")
        assertEquals(QuestionMode.UNKNOWN, guidanceResult.primaryMode)
    }

    @Test
    fun testUnknownBehavior() {
        // Unknown, completely random prompt without anchors
        val unknownResult = classifier.classify("Oranges are a healthy fruit")
        assertEquals(QuestionMode.UNKNOWN, unknownResult.primaryMode)
        assertEquals(0.5, unknownResult.modeConfidence, 0.01)
    }
}
