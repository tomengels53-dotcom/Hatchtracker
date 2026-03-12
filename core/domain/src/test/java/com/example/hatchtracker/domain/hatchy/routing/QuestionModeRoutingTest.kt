package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.HatchyOrchestrator
import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.HatchyConfidenceEvaluator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class QuestionModeRoutingTest {

    private lateinit var orchestrator: HatchyOrchestrator
    private lateinit var classifier: HatchyIntentClassifier
    private lateinit var entityExtractor: HatchyEntityExtractor
    private lateinit var questionModeClassifier: QuestionModeClassifier
    private lateinit var topicInferenceEngine: TopicInferenceEngine
    private lateinit var resolverRegistry: HatchyResolverRegistry
    private lateinit var responseComposer: HatchyResponseComposer

    private lateinit var appResolver: HatchyResolver
    private lateinit var realWorldResolver: HatchyResolver

    @Before
    fun setup() {
        classifier = mockk()
        entityExtractor = mockk()
        questionModeClassifier = mockk()
        topicInferenceEngine = mockk()
        resolverRegistry = mockk()
        responseComposer = mockk()

        appResolver = mockk()
        realWorldResolver = mockk()

        every { appResolver.capabilities } returns ResolverCapabilities(
            preferredQuestionModes = setOf(QuestionMode.APP_WORKFLOW),
            priority = 50
        )
        every { appResolver.score(any(), any()) } returns ScoreResult(
            finalScore = 0.8,
            components = ResolverScoreComponents(),
            entityRequirementSatisfied = true
        )
        coEvery { appResolver.resolve(any(), any()) } returns ResolverOutcome.Resolved(
            HatchyAnswer("App Answer", AnswerType.NAVIGATION, AnswerConfidence.HIGH, AnswerSource.APP_KNOWLEDGE_BASE, debugMetadata = mapOf("resolver" to "AppResolver"))
        )

        every { realWorldResolver.capabilities } returns ResolverCapabilities(
            preferredQuestionModes = setOf(QuestionMode.REAL_WORLD_GUIDANCE),
            priority = 50
        )
        every { realWorldResolver.score(any(), any()) } returns ScoreResult(
            finalScore = 0.8,
            components = ResolverScoreComponents(),
            entityRequirementSatisfied = true
        )
        coEvery { realWorldResolver.resolve(any(), any()) } returns ResolverOutcome.Resolved(
            HatchyAnswer("RealWorld Answer", AnswerType.GUIDANCE, AnswerConfidence.HIGH, AnswerSource.POULTRY_KNOWLEDGE_BASE, debugMetadata = mapOf("resolver" to "RealWorldResolver"))
        )

        every { resolverRegistry.getAllResolvers() } returns listOf(appResolver, realWorldResolver)
        every { responseComposer.compose(any(), any(), any()) } answers { it.invocation.args[0] as HatchyAnswer }

        val contextBuilder: HatchyContextSnapshotBuilder = mockk()
        every { contextBuilder.build(any(), any()) } returns mockk()

        val pacingPolicy: ResponsePacingPolicy = mockk()
        val confEvaluator: HatchyConfidenceEvaluator = mockk()

        orchestrator = HatchyOrchestrator(
            classifier, questionModeClassifier, entityExtractor,
            contextBuilder, resolverRegistry, responseComposer,
            confEvaluator, topicInferenceEngine, pacingPolicy
        )
    }

    @Test
    fun `App Workflow query biases AppResolver over RealWorldResolver`() = runBlocking {
        // App Workflow scenario
        every { questionModeClassifier.classify(any()) } returns QuestionModeResult(QuestionMode.APP_WORKFLOW, null, 1.0, 1.0, 0.0, 0.0)
        every { classifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.APP_NAVIGATION, confidence = 0.9)
        every { entityExtractor.extract(any()) } returns emptyList()
        every { topicInferenceEngine.inferTopics(any(), any(), any()) } returns TopicInferenceResult(null, null, emptyMap(), 1.0)

        val answer = orchestrator.processQuery("Where in the app do I start a new incubation?", mockk())
        
        // Ensure AppResolver won the tie due to QuestionMode bias
        assertEquals("App Answer", answer.text)
    }

    @Test
    fun `Real World query biases RealWorldResolver over AppResolver`() = runBlocking {
        // Real World scenario
        every { questionModeClassifier.classify(any()) } returns QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        every { classifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.INCUBATION_GUIDANCE, confidence = 0.9)
        every { entityExtractor.extract(any()) } returns emptyList()
        every { topicInferenceEngine.inferTopics(any(), any(), any()) } returns TopicInferenceResult(null, null, emptyMap(), 1.0)

        val answer = orchestrator.processQuery("I bought a new incubator, how do I set it up?", mockk())
        
        // Ensure RealWorldResolver won the tie due to QuestionMode bias
        assertEquals("RealWorld Answer", answer.text)
    }
}
