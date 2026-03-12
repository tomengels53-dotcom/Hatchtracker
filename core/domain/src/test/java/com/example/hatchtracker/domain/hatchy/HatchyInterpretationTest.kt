package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.model.*


import com.example.hatchtracker.data.models.SubscriptionTier
import app.cash.turbine.test
import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.routing.AnswerConfidence
import com.example.hatchtracker.domain.hatchy.routing.AnswerSource
import com.example.hatchtracker.domain.hatchy.routing.AnswerType
import com.example.hatchtracker.domain.hatchy.routing.EntityType
import com.example.hatchtracker.domain.hatchy.routing.HatchyAnswer
import com.example.hatchtracker.domain.hatchy.routing.HatchyContextSnapshot
import com.example.hatchtracker.domain.hatchy.routing.HatchyEntity
import com.example.hatchtracker.domain.hatchy.routing.HatchyIntent
import com.example.hatchtracker.domain.hatchy.routing.HatchyIntentClassifier
import com.example.hatchtracker.domain.hatchy.routing.HatchyIntentResult
import com.example.hatchtracker.domain.hatchy.routing.HatchyProcessEvent
import com.example.hatchtracker.domain.hatchy.routing.HatchyResolverRegistry
import com.example.hatchtracker.domain.hatchy.routing.HatchyEntityExtractor
import com.example.hatchtracker.domain.hatchy.routing.HatchyContextSnapshotBuilder
import com.example.hatchtracker.domain.hatchy.routing.QueryInterpretation
import com.example.hatchtracker.domain.hatchy.routing.QuestionMode
import com.example.hatchtracker.domain.hatchy.routing.QuestionModeClassifier
import com.example.hatchtracker.domain.hatchy.routing.QuestionModeResult
import com.example.hatchtracker.domain.hatchy.routing.ResponsePacingPolicy
import com.example.hatchtracker.domain.hatchy.routing.TopicInferenceEngine
import com.example.hatchtracker.domain.hatchy.routing.TopicInferenceResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HatchyInterpretationTest {

    private lateinit var orchestrator: HatchyOrchestrator
    private lateinit var classifier: HatchyIntentClassifier
    private lateinit var entityExtractor: HatchyEntityExtractor
    private lateinit var questionModeClassifier: QuestionModeClassifier
    private lateinit var topicInferenceEngine: TopicInferenceEngine
    private lateinit var responseComposer: HatchyResponseComposer
    private lateinit var contextBuilder: HatchyContextSnapshotBuilder
    private lateinit var resolverRegistry: HatchyResolverRegistry
    private lateinit var confidenceEvaluator: HatchyConfidenceEvaluator
    private lateinit var pacingPolicy: ResponsePacingPolicy

    @Before
    fun setup() {
        classifier = mockk()
        entityExtractor = mockk()
        questionModeClassifier = mockk()
        topicInferenceEngine = mockk()
        responseComposer = mockk()
        contextBuilder = mockk()
        resolverRegistry = mockk()
        confidenceEvaluator = mockk()
        pacingPolicy = mockk()

        orchestrator = HatchyOrchestrator(
            classifier = classifier,
            questionModeClassifier = questionModeClassifier,
            entityExtractor = entityExtractor,
            contextBuilder = contextBuilder,
            resolverRegistry = resolverRegistry,
            responseComposer = responseComposer,
            confidenceEvaluator = confidenceEvaluator,
            topicInferenceEngine = topicInferenceEngine,
            pacingPolicy = pacingPolicy
        )
    }

    @Test
    fun `orchestrator should assemble QueryInterpretation correctly`() = runTest {
        val query = "how long to hatch"
        val mode = QuestionModeResult(
            primaryMode = QuestionMode.REAL_WORLD_GUIDANCE,
            secondaryMode = null,
            modeConfidence = 0.9,
            appAnchorScore = 0.0,
            realWorldAnchorScore = 1.0,
            userDataAnchorScore = 0.0
        )
        val intent = HatchyIntentResult(intent = HatchyIntent.INCUBATION_GUIDANCE, confidence = 0.8)
        val entities = listOf(
            HatchyEntity(
                type = EntityType.INCUBATION_TOPIC,
                value = "HATCH_TIMING",
                originalText = "hatch",
                confidence = 1.0
            )
        )
        val topic = TopicInferenceResult(
            primaryTopic = KnowledgeTopic.HATCH_TIMING,
            secondaryTopic = null,
            topicScores = mapOf(KnowledgeTopic.HATCH_TIMING to 1.0),
            confidence = 1.0
        )
        val interpretationSlot = slot<QueryInterpretation>()

        every { questionModeClassifier.classify(query) } returns mode
        every { classifier.classify(query, any()) } returns intent
        every { entityExtractor.extract(query) } returns entities
        every { topicInferenceEngine.inferTopics(query, entities, mode) } returns topic
        every { pacingPolicy.determineThinkingLabel(capture(interpretationSlot)) } returns "Thinking..."
        every { contextBuilder.build(any(), any()) } returns testSnapshot()
        every { resolverRegistry.getAllResolvers() } returns emptyList()
        coEvery { responseComposer.compose(any(), any(), any()) } answers { firstArg() }
        every { pacingPolicy.calculateMinimumDelay(any()) } returns 0L

        orchestrator.processQueryFlow(query, HatchyContext(localeTag = "en")).test {
            assertTrue(awaitItem() is HatchyProcessEvent.Thinking)
            val done = awaitItem() as HatchyProcessEvent.Done
            assertEquals(AnswerType.FALLBACK, done.answer.type)
            awaitComplete()
        }

        val interpretation = interpretationSlot.captured
        assertEquals(query, interpretation.rawQuery)
        assertEquals(mode, interpretation.questionMode)
        assertEquals(intent.intent, interpretation.intent)
        assertEquals(entities, interpretation.entities)
        assertEquals(topic, interpretation.topicResult)
        assertEquals(emptyList<Any>(), interpretation.inferredGoals)
    }

    private fun testSnapshot() = HatchyContextSnapshot(
        currentModule = "HOME",
        selectedSpecies = null,
        tier = SubscriptionTier.FREE,
        isAdminOrDeveloper = false,
        recentBreedsMentioned = emptyList(),
        recentTraitsMentioned = emptyList(),
        lastResult = null,
        hasUserDataContext = false
    )
}
