package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.*
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResolverScoringTest {

    private lateinit var knowledgeResolver: IncubationGuidanceResolver
    private lateinit var statusResolver: IncubationStatusResolver
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
        knowledgeResolver = IncubationGuidanceResolver(mockk(), responseComposer)
        statusResolver = IncubationStatusResolver(mockk(), responseComposer)
    }

    @Test
    fun `KnowledgeResolver weights topicMatch more heavily than statusResolver`() {
        val interpretation = QueryInterpretation(
            rawQuery = "incubation temperature",
            questionMode = QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0),
            entities = emptyList(),
            intent = HatchyIntent.INCUBATION_GUIDANCE,
            topicResult = TopicInferenceResult(KnowledgeTopic.TEMPERATURE, null, mapOf(KnowledgeTopic.TEMPERATURE to 1.0), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.9
        )
        val context = testContext()

        val scoreKnowledge = knowledgeResolver.score(interpretation, context)
        
        // Knowledge weights: topicMatch=0.5, mode=0.3, entity=0.2
        // components: topic=1.0, mode=1.0, entity=0.0 (no required entities in GuidanceResolver)
        // finalScore = (1.0 * 0.5) + (0.0 * 0.2) + (1.0 * 0.3) + ... = 0.8 + tiny priority
        assertTrue("Knowledge score should be high for knowledge query", scoreKnowledge.finalScore > 0.7)
    }

    @Test
    fun `StatusResolver weights UserData more heavily`() {
        val interpretation = QueryInterpretation(
            rawQuery = "how is my batch doing?",
            questionMode = QuestionModeResult(QuestionMode.USER_DATA_STATUS, null, 1.0, 1.0, 0.0, 1.0),
            entities = listOf(HatchyEntity(EntityType.USER_DATA_REF, "batch_1", "my batch")),
            intent = HatchyIntent.INCUBATION_STATUS,
            topicResult = TopicInferenceResult(DataTopic.ACTIVE_BATCH_STATUS, null, mapOf(DataTopic.ACTIVE_BATCH_STATUS to 1.0), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.9
        )
        val context = testContext()

        val scoreStatus = statusResolver.score(interpretation, context)
        
        // Query weight: userData=0.6, topic=0.4
        // components: topic=1.0, userData=1.0
        // finalScore = (1.0 * 0.4) + (1.0 * 0.6) = 1.0 + tiny priority
        assertTrue("Status score should be near 1.0 for data query", scoreStatus.finalScore > 0.9)
    }

    @Test
    fun `Missing required entities zero out score unless fallback allowed`() {
        // IncubationStatusResolver requires USER_DATA_REF
        val interpretation = QueryInterpretation(
            rawQuery = "Hatch timing", // Ambiguous, no batch ref
            questionMode = QuestionModeResult(QuestionMode.USER_DATA_STATUS, null, 1.0, 0.5, 0.5, 0.5),
            entities = emptyList(), // Missing USER_DATA_REF
            intent = HatchyIntent.INCUBATION_STATUS,
            topicResult = TopicInferenceResult(KnowledgeTopic.HATCH_TIMING, null, mapOf(KnowledgeTopic.HATCH_TIMING to 1.0), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.8
        )
        val context = testContext()

        val score = statusResolver.score(interpretation, context)
        
        assertEquals("Score should be zeroed for missing required entities without fallback", 0.0, score.finalScore, 0.001)
        assertEquals(false, score.entityRequirementSatisfied)
    }

    @Test
    fun `Approximate matching allows non-zero score even if entities missing`() {
        // IncubationGuidanceResolver allowsApproximateMatch = true
        val interpretation = QueryInterpretation(
            rawQuery = "temperature",
            questionMode = QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0),
            entities = emptyList(), // Optional entities for this resolver
            intent = HatchyIntent.INCUBATION_GUIDANCE,
            topicResult = TopicInferenceResult(KnowledgeTopic.TEMPERATURE, null, mapOf(KnowledgeTopic.TEMPERATURE to 1.0), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.8
        )
        val context = testContext()

        val score = knowledgeResolver.score(interpretation, context)
        
        assertTrue("Guidance should have non-zero score for topic-only query", score.finalScore > 0.4)
        assertTrue(score.entityRequirementSatisfied) // because they are optional
    }
}
