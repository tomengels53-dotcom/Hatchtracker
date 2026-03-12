package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.HatchyConfidenceEvaluator
import com.example.hatchtracker.domain.hatchy.HatchyOrchestrator
import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.routing.services.BreedingGuidanceKnowledgeService
import com.example.hatchtracker.domain.hatchy.routing.services.EvidenceMetadata
import com.example.hatchtracker.domain.hatchy.routing.services.IncubationKnowledgeService
import com.example.hatchtracker.domain.hatchy.routing.services.KnowledgeMatchResult
import com.example.hatchtracker.domain.hatchy.routing.services.PoultryKnowledgeService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HatchySmokeTest {

    private lateinit var orchestrator: HatchyOrchestrator
    private lateinit var questionModeClassifier: QuestionModeClassifier
    private lateinit var intentClassifier: HatchyIntentClassifier
    private lateinit var entityExtractor: HatchyEntityExtractor
    private lateinit var topicEngine: TopicInferenceEngine
    private lateinit var responseComposer: HatchyResponseComposer
    private lateinit var resolverRegistry: HatchyResolverRegistry

    private lateinit var incubationKnowledgeService: IncubationKnowledgeService
    private lateinit var breedingGuidanceService: BreedingGuidanceKnowledgeService
    private lateinit var poultryService: PoultryKnowledgeService

    @Before
    fun setup() {
        questionModeClassifier = mockk()
        intentClassifier = mockk()
        entityExtractor = mockk()
        topicEngine = mockk()
        responseComposer = mockk()
        resolverRegistry = mockk()
        incubationKnowledgeService = mockk()
        breedingGuidanceService = mockk()
        poultryService = mockk()

        val incubationGuidanceResolver = IncubationGuidanceResolver(incubationKnowledgeService, responseComposer)
        val breedingGuidanceResolver = BreedingGuidanceResolver(breedingGuidanceService, responseComposer)
        val poultryKnowledgeResolver = PoultryKnowledgeResolver(poultryService, responseComposer)
        val fallbackResolver = FallbackResolver(responseComposer)

        every { resolverRegistry.getAllResolvers() } returns listOf(
            incubationGuidanceResolver,
            breedingGuidanceResolver,
            poultryKnowledgeResolver,
            fallbackResolver
        )
        every { responseComposer.compose(any(), any(), any()) } answers { it.invocation.args[0] as HatchyAnswer }

        orchestrator = HatchyOrchestrator(
            intentClassifier,
            questionModeClassifier,
            entityExtractor,
            HatchyContextSnapshotBuilder(),
            resolverRegistry,
            responseComposer,
            mockk<HatchyConfidenceEvaluator>(),
            topicEngine,
            mockk(relaxed = true)
        )
    }

    @Test
    fun `Regression - How long is the incubation period for chickens`() = runBlocking {
        val query = "How long is the incubation period for chickens?"
        stubInterpretation(
            query = query,
            intent = HatchyIntent.INCUBATION_GUIDANCE,
            questionMode = QuestionMode.REAL_WORLD_GUIDANCE,
            topic = KnowledgeTopic.INCUBATION_PERIOD,
            entities = listOf(HatchyEntity(EntityType.POULTRY_SPECIES, PoultrySpecies.CHICKEN.name, "chickens"))
        )
        coEvery { incubationKnowledgeService.findMatch(any(), any(), any(), any()) } returns KnowledgeMatchResult(
            content = "Chicken eggs take 21 days.",
            confidence = 0.9,
            source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(1.0)
        )

        val answer = orchestrator.processQuery(query, HatchyContext())

        assertEquals(AnswerType.INCUBATION, answer.type)
        assertTrue(answer.text.contains("21 days"))
    }

    @Test
    fun `Regression - Why are my chicks in the 2nd generation not the same color`() = runBlocking {
        val query = "Why are my chicks in the 2nd generation not the same color?"
        stubInterpretation(
            query = query,
            intent = HatchyIntent.BREEDING_GUIDANCE,
            questionMode = QuestionMode.REAL_WORLD_GUIDANCE,
            topic = KnowledgeTopic.GENERATION_VARIATION
        )
        coEvery { breedingGuidanceService.findMatch(any(), any(), any(), any()) } returns KnowledgeMatchResult(
            content = "F2 segregation causes color variation.",
            confidence = 0.9,
            source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(1.0)
        )

        val answer = orchestrator.processQuery(query, HatchyContext())

        assertEquals(AnswerType.GUIDANCE, answer.type)
        assertTrue(answer.text.contains("F2 segregation"))
    }

    @Test
    fun `Regression - I bought a new incubator how do I set it up`() = runBlocking {
        val query = "I bought a new incubator, how do I set it up?"
        stubInterpretation(
            query = query,
            intent = HatchyIntent.GENERAL_POULTRY,
            questionMode = QuestionMode.REAL_WORLD_GUIDANCE,
            topic = KnowledgeTopic.SETUP_DEVICE
        )
        coEvery { poultryService.findMatch(any(), any(), any(), any()) } returns KnowledgeMatchResult(
            content = "Place your incubator in a room with stable temperature.",
            confidence = 0.9,
            source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(1.0)
        )

        val answer = orchestrator.processQuery(query, HatchyContext())

        assertEquals(AnswerType.POULTRY_KNOWLEDGE, answer.type)
        assertTrue(answer.text.contains("stable temperature"))
    }

    @Test
    fun `Regression - Unknown routing falls back cleanly`() = runBlocking {
        val query = "What breeds should I cross for excellent egg layers and color?"
        stubInterpretation(
            query = query,
            intent = HatchyIntent.OTHER,
            questionMode = QuestionMode.UNKNOWN,
            topic = null
        )

        val answer = orchestrator.processQuery(query, HatchyContext())

        assertEquals(AnswerSource.FALLBACK, answer.source)
        assertEquals(AnswerType.FALLBACK, answer.type)
    }

    @Test
    fun `Regression - Where in the app do I start a new incubation`() = runBlocking {
        val query = "Where in the app do I start a new incubation?"
        stubInterpretation(
            query = query,
            intent = HatchyIntent.APP_NAVIGATION,
            questionMode = QuestionMode.APP_WORKFLOW,
            topic = WorkflowTopic.START_INCUBATION
        )

        val answer = orchestrator.processQuery(query, HatchyContext())

        assertEquals(AnswerSource.FALLBACK, answer.source)
    }

    private fun stubInterpretation(
        query: String,
        intent: HatchyIntent,
        questionMode: QuestionMode,
        topic: HatchyTopic?,
        entities: List<HatchyEntity> = emptyList()
    ) {
        every { questionModeClassifier.classify(query) } returns QuestionModeResult(
            primaryMode = questionMode,
            secondaryMode = null,
            modeConfidence = if (questionMode == QuestionMode.UNKNOWN) 0.5 else 1.0,
            appAnchorScore = if (questionMode == QuestionMode.APP_WORKFLOW) 1.0 else 0.0,
            realWorldAnchorScore = if (questionMode == QuestionMode.REAL_WORLD_GUIDANCE) 1.0 else 0.0,
            userDataAnchorScore = if (questionMode == QuestionMode.USER_DATA_STATUS) 1.0 else 0.0
        )
        every { intentClassifier.classify(query, any()) } returns HatchyIntentResult(intent = intent, confidence = 0.9)
        every { entityExtractor.extract(query) } returns entities
        every { topicEngine.inferTopics(query, entities, any()) } returns TopicInferenceResult(
            primaryTopic = topic,
            secondaryTopic = null,
            topicScores = if (topic != null) mapOf(topic to 1.0) else emptyMap(),
            confidence = if (topic != null) 1.0 else 0.0
        )
    }
}
