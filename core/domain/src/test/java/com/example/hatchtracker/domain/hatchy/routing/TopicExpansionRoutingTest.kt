package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.*
import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.routing.services.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class TopicExpansionRoutingTest {

    private lateinit var orchestrator: HatchyOrchestrator
    private lateinit var lexiconRegistry: ILexiconRegistry
    private lateinit var topicInferenceEngine: TopicInferenceEngine
    private lateinit var entityExtractor: HatchyEntityExtractor
    private lateinit var intentClassifier: HatchyIntentClassifier
    private lateinit var questionModeClassifier: QuestionModeClassifier
    
    // Services
    private lateinit var incubationKnowledgeService: IncubationKnowledgeService
    private lateinit var incubationQueryService: IncubationQueryService
    private lateinit var breedingGuidanceService: BreedingGuidanceKnowledgeService
    private lateinit var nurseryKnowledgeService: NurseryKnowledgeService
    private lateinit var breedRepo: IBreedStandardRepository
    private lateinit var advisorService: FlockBreedingAdvisorService
    
    // Resolvers
    private lateinit var incubationGuidanceResolver: IncubationGuidanceResolver
    private lateinit var incubationStatusResolver: IncubationStatusResolver
    private lateinit var breedingGuidanceResolver: BreedingGuidanceResolver
    private lateinit var breedKnowledgeResolver: BreedKnowledgeResolver
    private lateinit var advisorResolver: FlockBreedingAdvisorResolver
    private lateinit var fallbackResolver: FallbackResolver

    private lateinit var contextBuilder: HatchyContextSnapshotBuilder

    @Before
    fun setup() {
        lexiconRegistry = LexiconRegistry()
        topicInferenceEngine = TopicInferenceEngine(lexiconRegistry)
        
        breedRepo = mockk()
        every { breedRepo.getAllBreeds() } returns emptyList()
        entityExtractor = mockk()
        every { entityExtractor.extract(any()) } returns emptyList()
        
        intentClassifier = mockk()
        questionModeClassifier = mockk()
        
        val responseComposer = mockk<HatchyResponseComposer>(relaxed = true)
        every { responseComposer.compose(any(), any(), any()) } answers { it.invocation.args[0] as HatchyAnswer }
        
        incubationKnowledgeService = mockk()
        incubationQueryService = mockk()
        breedingGuidanceService = mockk()
        nurseryKnowledgeService = mockk()
        advisorService = mockk()
        val pacingPolicy = mockk<ResponsePacingPolicy>(relaxed = true)
        
        val mockEvidence = EvidenceMetadata(1.0)
        
        coEvery { incubationKnowledgeService.findMatch(any(), any(), any(), any()) } returns KnowledgeMatchResult(
            content = "Incubation period is 21 days",
            confidence = 1.0,
            source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
            evidence = mockEvidence
        )
        coEvery { incubationQueryService.resolveIncubationStatusQuery(any(), any(), any()) } returns QueryResolutionResult(
            data = emptyMap(),
            summary = "Your batch is on Day 18",
            confidence = 1.0,
            source = AnswerSource.USER_DATA,
            evidence = mockEvidence
        )
        coEvery { breedingGuidanceService.findMatch(any(), any(), any(), any()) } returns KnowledgeMatchResult(
            content = "Generational variation occurs...",
            confidence = 1.0,
            source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
            evidence = mockEvidence
        )
        every { pacingPolicy.determineThinkingLabel(any()) } returns "Thinking..."
        every { pacingPolicy.calculateMinimumDelay(any()) } returns 0L
        
        incubationGuidanceResolver = IncubationGuidanceResolver(incubationKnowledgeService, responseComposer)
        incubationStatusResolver = IncubationStatusResolver(incubationQueryService, responseComposer)
        breedingGuidanceResolver = BreedingGuidanceResolver(breedingGuidanceService, responseComposer)
        breedKnowledgeResolver = BreedKnowledgeResolver(breedRepo, responseComposer)
        advisorResolver = FlockBreedingAdvisorResolver(advisorService, responseComposer)
        fallbackResolver = FallbackResolver(responseComposer)
        val nurseryGuidanceResolver = NurseryGuidanceResolver(nurseryKnowledgeService, responseComposer)
        
        val resolverRegistry = mockk<HatchyResolverRegistry>()
        every { resolverRegistry.getAllResolvers() } returns listOf(
            incubationGuidanceResolver, incubationStatusResolver,
            breedingGuidanceResolver, breedKnowledgeResolver,
            nurseryGuidanceResolver, advisorResolver, fallbackResolver
        )
        
        contextBuilder = HatchyContextSnapshotBuilder()
        
        orchestrator = HatchyOrchestrator(
            intentClassifier, questionModeClassifier, entityExtractor,
            contextBuilder, resolverRegistry, responseComposer,
            mockk(), topicInferenceEngine, pacingPolicy
        )
    }

    @Test
    fun `How long is the incubation period for chickens routes to IncubationGuidanceResolver`() = runBlocking {
        // Mock classifiers
        every { questionModeClassifier.classify(any()) } returns QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        every { intentClassifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.INCUBATION_GUIDANCE, confidence = 0.9)
        every { entityExtractor.extract("How long is the incubation period for chickens?") } returns listOf(
            HatchyEntity(EntityType.POULTRY_SPECIES, PoultrySpecies.CHICKEN.name, "chickens")
        )
        
        val answer = orchestrator.processQuery("How long is the incubation period for chickens?", HatchyContext())
        
        assertEquals(AnswerType.INCUBATION, answer.type)
        assertEquals("IncubationGuidanceResolver", answer.debugMetadata?.get("resolver"))
        assertEquals("Incubation period is 21 days", answer.text)
    }

    @Test
    fun `When do my current batch eggs hatch routes to IncubationStatusResolver via QuestionMode boost`() = runBlocking {
        every { questionModeClassifier.classify(any()) } returns QuestionModeResult(QuestionMode.USER_DATA_STATUS, null, 1.0, 1.0, 0.0, 1.0)
        every { intentClassifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.INCUBATION_STATUS, confidence = 0.9)
        every { entityExtractor.extract("When do my current batch eggs hatch?") } returns listOf(
            HatchyEntity(EntityType.USER_DATA_REF, "active_batch", "current batch")
        )
        
        val answer = orchestrator.processQuery("When do my current batch eggs hatch?", HatchyContext())
        
        assertEquals(AnswerType.INCUBATION, answer.type)
        assertEquals("IncubationStatusResolver", answer.debugMetadata?.get("resolver"))
        assertEquals("Your batch is on Day 18", answer.text)
    }

    @Test
    fun `Why are my chicks in the 2nd generation not the same color routes to BreedingGuidanceResolver`() = runBlocking {
        every { questionModeClassifier.classify(any()) } returns QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        every { intentClassifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.BREEDING_GUIDANCE, confidence = 0.9)
        
        val answer = orchestrator.processQuery("Why are my chicks in the 2nd generation not the same color?", HatchyContext())
        
        assertEquals(AnswerType.GUIDANCE, answer.type)
        assertEquals("BreedingGuidanceResolver", answer.debugMetadata?.get("resolver"))
    }

    @Test
    fun `What breeds should I cross for excellent egg layers and color routes to BreedKnowledgeResolver`() = runBlocking {
        // Here we simulate the goal-based pairing routing
        every { questionModeClassifier.classify(any()) } returns QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        every { intentClassifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.BREED_INFO, confidence = 0.9)
        every { entityExtractor.extract("What breeds should I cross for excellent egg layers and color?") } returns listOf(
            HatchyEntity(EntityType.TRAIT, "egg_production", "egg layers"),
            HatchyEntity(EntityType.TRAIT, "color", "color")
        )
        
        val answer = orchestrator.processQuery("What breeds should I cross for excellent egg layers and color?", HatchyContext())
        
        assertEquals("BreedKnowledgeResolver", answer.debugMetadata?.get("resolver"))
    }
    
    @Test
    fun `What breeds should I cross does NOT route to FlockBreedingAdvisorResolver`() = runBlocking {
        every { questionModeClassifier.classify(any()) } returns QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        every { intentClassifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.BREEDING_GUIDANCE, confidence = 0.9)
        
        val answer = orchestrator.processQuery("What breeds should I cross?", HatchyContext())
        
        assertNotEquals("FlockBreedingAdvisorResolver", answer.debugMetadata?.get("resolver"))
    }
}
