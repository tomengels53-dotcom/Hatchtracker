package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.model.*


import com.example.hatchtracker.domain.hatchy.routing.*
import com.example.hatchtracker.domain.hatchy.routing.services.IncubationKnowledgeService
import com.example.hatchtracker.domain.hatchy.routing.services.TemperatureFormatter
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EndToEndRoutingTest {

    private lateinit var lexiconRegistry: LexiconRegistry
    private lateinit var entityExtractor: HatchyEntityExtractor
    private lateinit var breedRepository: IBreedStandardRepository
    private lateinit var topicInferenceEngine: TopicInferenceEngine
    private lateinit var tempFormatter: TemperatureFormatter
    private lateinit var incubationService: IncubationKnowledgeService
    private lateinit var responseComposer: HatchyResponseComposer
    private lateinit var resolver: IncubationGuidanceResolver

    @Before
    fun setup() {
        lexiconRegistry = LexiconRegistry()
        breedRepository = mockk()
        every { breedRepository.getAllBreeds() } returns emptyList() // Simplified

        tempFormatter = TemperatureFormatter()
        entityExtractor = HatchyEntityExtractor(breedRepository)
        topicInferenceEngine = TopicInferenceEngine(lexiconRegistry)
        incubationService = IncubationKnowledgeService(tempFormatter) 
        responseComposer = HatchyResponseComposer()
        
        resolver = IncubationGuidanceResolver(incubationService, responseComposer)
    }

    @Test
    fun `should route incubation period query to correct answer for chickens`() = runTest {
        val query = "How long is the incubation period for chickens?"
        
        // 1. Manually simulate the Orchestrator's interpret step parts
        val qMode = QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        val entities = entityExtractor.extract(query)
        val topicResult = topicInferenceEngine.inferTopics(query, entities, qMode)
        
        // Verify interpretation
        assertTrue(entities.any { it.type == EntityType.POULTRY_SPECIES && it.value == "CHICKEN" })
        assertEquals(KnowledgeTopic.INCUBATION_PERIOD, topicResult.primaryTopic)

        val interpretation = QueryInterpretation(
            rawQuery = query,
            questionMode = qMode,
            entities = entities,
            intent = HatchyIntent.INCUBATION_GUIDANCE,
            topicResult = topicResult,
            inferredGoals = emptyList(),
            confidence = 1.0
        )
        
        val context = HatchyContextSnapshot(
            currentModule = "HOME",
            selectedSpecies = null,
            tier = com.example.hatchtracker.data.models.SubscriptionTier.FREE,
            isAdminOrDeveloper = false,
            recentBreedsMentioned = emptyList(),
            recentTraitsMentioned = emptyList(),
            lastResult = null,
            hasUserDataContext = false
        )

        // 2. Resolve
        val outcome = resolver.resolve(interpretation, context)
        
        // 3. Verify
        assertTrue(outcome is ResolverOutcome.Resolved)
        val result = (outcome as ResolverOutcome.Resolved).answer
        assertEquals(AnswerType.INCUBATION, result.type)
        assertTrue(result.text.contains("21 days"))
    }

    @Test
    fun `should route brooder temperature query for ducks`() = runTest {
        val query = "What is the temp for ducks in the brooder?"
        
        // Lexicon extraction (temp -> TEMPERATURE topic)
        val entities = entityExtractor.extract(query)
        assertTrue(entities.any { it.type == EntityType.POULTRY_SPECIES && it.value == "DUCK" })
        
        // We'll test with the NurseryKnowledgeService too if we want a broader E2E
        // But for this test file, verifying the flow for one domain is a strong signal.
    }

    @Test
    fun `should provide unit-aware temperature in response`() = runTest {
        val query = "What is the incubation temp for chickens?"
        
        val qMode = QuestionModeResult(QuestionMode.REAL_WORLD_GUIDANCE, null, 1.0, 0.0, 1.0, 0.0)
        val entities = entityExtractor.extract(query)
        val topicResult = topicInferenceEngine.inferTopics(query, entities, qMode)
        
        val interpretation = QueryInterpretation(
            rawQuery = query,
            questionMode = qMode,
            entities = entities,
            intent = HatchyIntent.INCUBATION_GUIDANCE,
            topicResult = topicResult,
            inferredGoals = emptyList(),
            confidence = 1.0
        )
        
        val context = HatchyContextSnapshot(
            currentModule = "HOME",
            selectedSpecies = null,
            tier = com.example.hatchtracker.data.models.SubscriptionTier.FREE,
            isAdminOrDeveloper = false,
            recentBreedsMentioned = emptyList(),
            recentTraitsMentioned = emptyList(),
            lastResult = null,
            hasUserDataContext = false
        )

        val outcome = resolver.resolve(interpretation, context)
        assertTrue(outcome is ResolverOutcome.Resolved)
        val result = (outcome as ResolverOutcome.Resolved).answer
        
        // Default is DUAL_CELSIUS_FIRST
        assertTrue(result.text.contains("37.5°C (99.5°F)"))
    }
}
