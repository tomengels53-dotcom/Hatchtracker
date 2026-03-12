package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HatchyFallbackTest {

    private lateinit var guidanceResolver: IncubationGuidanceResolver
    private lateinit var statusResolver: IncubationStatusResolver
    private val incubationKnowledgeService = mockk<com.example.hatchtracker.domain.hatchy.routing.services.IncubationKnowledgeService>()
    private val responseComposer = mockk<HatchyResponseComposer>(relaxed = true)

    private fun testContext() = HatchyContextSnapshot(
        currentModule = "HOME",
        selectedSpecies = null,
        tier = com.example.hatchtracker.data.models.SubscriptionTier.FREE,
        isAdminOrDeveloper = false,
        recentBreedsMentioned = emptyList(),
        recentTraitsMentioned = emptyList(),
        lastResult = null,
        hasUserDataContext = false
    )

    @Before
    fun setup() {
        guidanceResolver = IncubationGuidanceResolver(incubationKnowledgeService, responseComposer)
        statusResolver = IncubationStatusResolver(mockk(), responseComposer)
    }

    @Test
    fun `GuidanceResolver returns InsufficientEvidence when species is missing and no direct match exists`() = runBlocking {
        val interpretation = QueryInterpretation(
            rawQuery = "What is the incubation temperature?",
            questionMode = QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0),
            entities = emptyList(),
            intent = HatchyIntent.INCUBATION_GUIDANCE,
            topicResult = TopicInferenceResult(KnowledgeTopic.TEMPERATURE, null, mapOf(KnowledgeTopic.TEMPERATURE to 1.0), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.9
        )

        coEvery {
            incubationKnowledgeService.findMatch(any(), null, "TEMPERATURE", any())
        } returns null

        val outcome = guidanceResolver.resolve(interpretation, testContext())

        assertTrue(outcome is ResolverOutcome.InsufficientEvidence)
    }

    @Test
    fun `Data resolvers score to zero when required user data is missing`() {
        val interpretation = QueryInterpretation(
            rawQuery = "How is my batch doing?",
            questionMode = QuestionModeResult(QuestionMode.USER_DATA_STATUS, null, 1.0, 1.0, 0.0, 1.0),
            entities = emptyList(),
            intent = HatchyIntent.INCUBATION_STATUS,
            topicResult = TopicInferenceResult(DataTopic.ACTIVE_BATCH_STATUS, null, mapOf(DataTopic.ACTIVE_BATCH_STATUS to 1.0), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.9
        )

        val score = statusResolver.score(interpretation, testContext())

        assertEquals(0.0, score.finalScore, 0.001)
        assertTrue(!score.entityRequirementSatisfied)
    }
}
