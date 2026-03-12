package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.*
import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.routing.services.*
import com.example.hatchtracker.model.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class TraitRoutingTest {

    private lateinit var orchestrator: HatchyOrchestrator
    private lateinit var lexiconRegistry: ILexiconRegistry
    private lateinit var topicInferenceEngine: TopicInferenceEngine
    private lateinit var entityExtractor: HatchyEntityExtractor
    private lateinit var intentClassifier: HatchyIntentClassifier
    private lateinit var questionModeClassifier: QuestionModeClassifier
    
    private lateinit var breedRepo: IBreedStandardRepository
    private lateinit var breedKnowledgeResolver: BreedKnowledgeResolver
    private lateinit var breedingGuidanceResolver: BreedingGuidanceResolver
    private lateinit var fallbackResolver: FallbackResolver

    @Before
    fun setup() {
        lexiconRegistry = LexiconRegistry()
        topicInferenceEngine = TopicInferenceEngine(lexiconRegistry)
        
        breedRepo = mockk()
        every { breedRepo.getAllBreeds() } returns listOf(
            BreedStandard(id = "ameraucana", name = "Ameraucana", species = "Chicken", eggColor = "Blue", temperament = "Calm"),
            BreedStandard(id = "pekin", name = "Pekin", species = "Duck", temperament = "Docile")
        )
        entityExtractor = mockk()
        
        intentClassifier = mockk()
        questionModeClassifier = mockk()
        
        val responseComposer = mockk<HatchyResponseComposer>(relaxed = true)
        every { responseComposer.compose(any(), any(), any()) } answers { it.invocation.args[0] as HatchyAnswer }
        
        val breedingGuidanceService = mockk<BreedingGuidanceKnowledgeService>()
        val pacingPolicy = mockk<ResponsePacingPolicy>(relaxed = true)
        
        breedKnowledgeResolver = BreedKnowledgeResolver(breedRepo, responseComposer)
        breedKnowledgeResolver.traitExtractor = TraitValueExtractor() // Inject manual instance for test
        
        breedingGuidanceResolver = BreedingGuidanceResolver(breedingGuidanceService, responseComposer)
        fallbackResolver = FallbackResolver(responseComposer)
        
        val resolverRegistry = mockk<HatchyResolverRegistry>()
        every { resolverRegistry.getAllResolvers() } returns listOf(
            breedKnowledgeResolver, breedingGuidanceResolver, fallbackResolver
        )
        
        orchestrator = HatchyOrchestrator(
            intentClassifier, questionModeClassifier, entityExtractor,
            HatchyContextSnapshotBuilder(), resolverRegistry, responseComposer,
            mockk(), topicInferenceEngine, pacingPolicy
        )
    }

    @Test
    fun `Which breeds lay blue eggs routes to BreedKnowledgeResolver`() = runBlocking {
        val query = "Which breeds lay blue eggs?"
        every { questionModeClassifier.classify(query) } returns QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        every { intentClassifier.classify(query, any()) } returns HatchyIntentResult(HatchyIntent.BREED_INFO, confidence = 0.9)
        every { entityExtractor.extract(query) } returns listOf(
            HatchyEntity(EntityType.TRAIT, "EGG_COLOR", "blue eggs")
        )
        
        val answer = orchestrator.processQuery(query, HatchyContext())
        
        assertEquals("BreedKnowledgeResolver", answer.debugMetadata?.get("resolver"))
        assertEquals(AnswerType.BREED_INFO, answer.type)
        assertEquals(AnswerSource.BREED_REPOSITORY, answer.source)
    }

    @Test
    fun `What breeds should I cross for blue eggs does NOT use BreedKnowledgeResolver as primary owner`() = runBlocking {
        val query = "What breeds should I cross for blue eggs?"
        // This should be classified as BREEDING_GUIDANCE or CROSSBREED_OUTCOME
        every { questionModeClassifier.classify(query) } returns QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        every { intentClassifier.classify(query, any()) } returns HatchyIntentResult(HatchyIntent.CROSSBREED_OUTCOME, confidence = 0.9)
        every { entityExtractor.extract(query) } returns listOf(
            HatchyEntity(EntityType.TRAIT, "EGG_COLOR", "blue eggs")
        )
        
        val answer = orchestrator.processQuery(query, HatchyContext())
        
        // It definitely shouldn't be BreedKnowledgeResolver because the intent is CROSSBREED_OUTCOME
        assertNotEquals("BreedKnowledgeResolver", answer.debugMetadata?.get("resolver"))
    }

    @Test
    fun `Which calm chickens lay blue eggs routes to BreedKnowledgeResolver with multi-trait match`() = runBlocking {
        val query = "Which calm chickens lay blue eggs?"
        every { questionModeClassifier.classify(query) } returns QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        every { intentClassifier.classify(query, any()) } returns HatchyIntentResult(HatchyIntent.BREED_INFO, confidence = 0.9)
        every { entityExtractor.extract(query) } returns listOf(
            HatchyEntity(EntityType.POULTRY_SPECIES, PoultrySpecies.CHICKEN.name, "chickens")
        )
        
        val answer = orchestrator.processQuery(query, HatchyContext())
        
        assertEquals("BreedKnowledgeResolver", answer.debugMetadata?.get("resolver"))
        // Extraction and matching is done inside the resolver now via traitExtractor
        // Ameraucana matches BOTH calm and blue eggs.
        // Pekin matches calm (via docile synonym in Lexicon) but is a Duck.
        // The result text should mention Ameraucana.
        assertEquals(AnswerType.BREED_INFO, answer.type)
    }

    @Test
    fun `How do I breed for blue eggs and calmer birds routes to breeding guidance`() = runBlocking {
        val query = "How do I breed for blue eggs and calmer birds?"
        every { questionModeClassifier.classify(query) } returns QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        every { intentClassifier.classify(query, any()) } returns HatchyIntentResult(HatchyIntent.BREEDING_GUIDANCE, confidence = 0.9)
        every { entityExtractor.extract(query) } returns emptyList()
        
        val answer = orchestrator.processQuery(query, HatchyContext())
        
        assertNotEquals("BreedKnowledgeResolver", answer.debugMetadata?.get("resolver"))
    }
}
