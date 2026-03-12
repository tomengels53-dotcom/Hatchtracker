package com.example.hatchtracker.domain.hatchy

import app.cash.turbine.test
import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.routing.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class HatchyOrchestratorPacingTest {

    private val classifier = mockk<HatchyIntentClassifier>()
    private val qModeClassifier = mockk<QuestionModeClassifier>()
    private val entityExtractor = mockk<HatchyEntityExtractor>()
    private val contextBuilder = mockk<HatchyContextSnapshotBuilder>()
    private val resolverRegistry = mockk<HatchyResolverRegistry>()
    private val responseComposer = mockk<HatchyResponseComposer>()
    private val confidenceEvaluator = mockk<HatchyConfidenceEvaluator>()
    private val topicInferenceEngine = mockk<TopicInferenceEngine>()
    private val pacingPolicy = mockk<ResponsePacingPolicy>()

    private lateinit var orchestrator: HatchyOrchestrator

    @Before
    fun setup() {
        orchestrator = HatchyOrchestrator(
            classifier, qModeClassifier, entityExtractor,
            contextBuilder, resolverRegistry, responseComposer,
            confidenceEvaluator, topicInferenceEngine, pacingPolicy
        )

        // Default mock behaviors - Synchronous
        every { qModeClassifier.classify(any()) } returns QuestionModeResult(QuestionMode.UNKNOWN, null, 0.5, 0.5, 0.5, 0.5)
        every { classifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.OTHER)
        every { entityExtractor.extract(any()) } returns emptyList()
        every { topicInferenceEngine.inferTopics(any(), any(), any()) } returns TopicInferenceResult(null, null, emptyMap(), 0.5)
        every { contextBuilder.build(any(), any()) } returns mockk()
        every { resolverRegistry.getAllResolvers() } returns emptyList()
        every { pacingPolicy.determineThinkingLabel(any()) } returns "Thinking..."
        every { pacingPolicy.calculateMinimumDelay(any()) } returns 500L
        
        // Default mock behaviors - Suspend
        coEvery { responseComposer.compose(any(), any(), any()) } returns HatchyAnswer("final", AnswerType.FALLBACK, AnswerConfidence.MEDIUM, AnswerSource.FALLBACK)
    }

    @Test
    fun `test pacing flow emits thinking then done with delay`() = runTest {
        val appContext = HatchyContext(localeTag = "en")
        
        orchestrator.processQueryFlow("hello", appContext).test {
            // Should emit Thinking immediately
            val thinking = awaitItem()
            assert(thinking is HatchyProcessEvent.Thinking)
            assertEquals("Thinking...", (thinking as HatchyProcessEvent.Thinking).label)

            // Advance time to just before delay ends
            advanceTimeBy(400)
            expectNoEvents()

            // Advance past the 500ms total delay
            advanceTimeBy(150)
            val done = awaitItem()
            assert(done is HatchyProcessEvent.Done)
            assertEquals("final", (done as HatchyProcessEvent.Done).answer.text)
            
            awaitComplete()
        }
    }

    @Test
    fun `test pacing adjusts based on resolved answer`() = runTest {
        val appContext = HatchyContext(localeTag = "en")
        
        val complexAnswer = HatchyAnswer("complex", AnswerType.RECOMMENDATION, AnswerConfidence.HIGH, AnswerSource.BREEDING_ENGINE)
        coEvery { responseComposer.compose(any(), any(), any()) } returns complexAnswer
        every { pacingPolicy.calculateMinimumDelay(complexAnswer) } returns 1500L

        orchestrator.processQueryFlow("complex query", appContext).test {
            awaitItem() // Thinking

            advanceTimeBy(1000)
            expectNoEvents()

            advanceTimeBy(600)
            val done = awaitItem()
            assertEquals("complex", (done as HatchyProcessEvent.Done).answer.text)
            
            awaitComplete()
        }
    }

    @Test
    fun `test navigation remains snappy`() = runTest {
        val appContext = HatchyContext(localeTag = "en")
        
        val navAnswer = HatchyAnswer("nav", AnswerType.NAVIGATION, AnswerConfidence.HIGH, AnswerSource.APP_KNOWLEDGE_BASE)
        coEvery { responseComposer.compose(any(), any(), any()) } returns navAnswer
        every { pacingPolicy.calculateMinimumDelay(navAnswer) } returns 300L

        orchestrator.processQueryFlow("go to settings", appContext).test {
            awaitItem() // Thinking

            advanceTimeBy(200)
            expectNoEvents()

            advanceTimeBy(150)
            val done = awaitItem()
            assertEquals("nav", (done as HatchyProcessEvent.Done).answer.text)
            
            awaitComplete()
        }
    }
}
