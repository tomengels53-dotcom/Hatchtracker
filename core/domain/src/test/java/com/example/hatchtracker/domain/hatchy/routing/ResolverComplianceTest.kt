package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

class ResolverComplianceTest {

    private lateinit var registry: HatchyResolverRegistry
    private val responseComposer: HatchyResponseComposer = mockk(relaxed = true)
    
    // Mocks for all services
    private val poultryService: PoultryKnowledgeService = mockk(relaxed = true)
    private val incubationKnowledgeService: IncubationKnowledgeService = mockk(relaxed = true)
    private val incubationQueryService: IncubationQueryService = mockk(relaxed = true)
    private val nurseryKnowledgeService: NurseryKnowledgeService = mockk(relaxed = true)
    private val nurseryQueryService: NurseryQueryService = mockk(relaxed = true)
    private val financeQueryService: FinanceQueryService = mockk(relaxed = true)
    private val equipmentQueryService: EquipmentQueryService = mockk(relaxed = true)
    private val breedingGuidanceService: BreedingGuidanceKnowledgeService = mockk(relaxed = true)
    private val breedingSimulationService: CrossbreedingSimulationService = mockk(relaxed = true)
    private val flockBreedingAdvisorService: FlockBreedingAdvisorService = mockk(relaxed = true)
    private val breedRepository: IBreedStandardRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        val appNavResolver = AppNavigationResolver(responseComposer)
        val breedKnowledgeResolver = BreedKnowledgeResolver(breedRepository, responseComposer)
        val breedingSimulationResolver = BreedingSimulationResolver(breedingSimulationService, responseComposer)
        val breedingGuidanceResolver = BreedingGuidanceResolver(breedingGuidanceService, responseComposer)
        val poultryKnowledgeResolver = PoultryKnowledgeResolver(poultryService, responseComposer)
        val incubationGuidanceResolver = IncubationGuidanceResolver(incubationKnowledgeService, responseComposer)
        val incubationStatusResolver = IncubationStatusResolver(incubationQueryService, responseComposer)
        val nurseryGuidanceResolver = NurseryGuidanceResolver(nurseryKnowledgeService, responseComposer)
        val nurseryStatusResolver = NurseryStatusResolver(nurseryQueryService, responseComposer)
        val financeHelpResolver = FinanceHelpResolver(financeQueryService, responseComposer)
        val financeSummaryResolver = FinanceSummaryResolver(financeQueryService, responseComposer)
        val equipmentHelpResolver = EquipmentHelpResolver(equipmentQueryService, responseComposer)
        val equipmentStatusResolver = EquipmentStatusResolver(equipmentQueryService, responseComposer)
        val flockBreedingAdvisorResolver = FlockBreedingAdvisorResolver(flockBreedingAdvisorService, responseComposer)
        val userDataAwareResolver = UserDataAwareResolver(nurseryQueryService, incubationQueryService, responseComposer)
        val fallbackResolver = FallbackResolver(responseComposer)

        registry = HatchyResolverRegistry(
            Provider { appNavResolver },
            Provider { breedKnowledgeResolver },
            Provider { breedingSimulationResolver },
            Provider { breedingGuidanceResolver },
            Provider { poultryKnowledgeResolver },
            Provider { incubationGuidanceResolver },
            Provider { incubationStatusResolver },
            Provider { nurseryGuidanceResolver },
            Provider { nurseryStatusResolver },
            Provider { financeHelpResolver },
            Provider { financeSummaryResolver },
            Provider { equipmentHelpResolver },
            Provider { equipmentStatusResolver },
            Provider { flockBreedingAdvisorResolver },
            Provider { userDataAwareResolver },
            Provider { fallbackResolver }
        )
    }

    @Test
    fun `registry sweep - all resolvers must provide truthful debug metadata`() = runBlocking {
        val resolvers = registry.getAllResolvers()
        val context = createTestContext()
        
        resolvers.forEach { resolver ->
            val selectedIntent = resolver.capabilities.supportedIntents.firstOrNull() ?: HatchyIntent.OTHER
            val interpretation = makeInterpretation(intent = selectedIntent, query = "test query")
            
            // We skip the ownership check for the sweep to force a resolve attempt
            val outcome = resolver.resolve(interpretation, context)
            
            if (outcome is ResolverOutcome.Resolved) {
                val metadata = outcome.answer.debugMetadata
                assertNotNull("Resolver ${resolver::class.java.simpleName} must provide debug metadata", metadata)
                assertTrue("Metadata must contain 'resolver' key", metadata!!.containsKey("resolver"))
                assertEquals("Resolver key must match class name", resolver::class.java.simpleName, metadata["resolver"])
                assertTrue("Metadata must contain 'outcome' key", metadata.containsKey("outcome"))
            }
        }
    }

    @Test
    fun `behavioral contract - incubation resolver must be query-aware`() = runBlocking {
        val resolver = registry.getResolver(HatchyIntent.INCUBATION_STATUS) as IncubationStatusResolver
        val context = createTestContext()
        
        // Scenario A: General Status
        every { runBlocking { incubationQueryService.resolveIncubationStatusQuery(null, null, context) } } returns QueryResolutionResult(
            data = mapOf("count" to 1),
            summary = "General summary",
            confidence = 1.0,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(matchScore = 1.0, matchedTopic = "GENERAL")
        )
        
        val interpretationA = makeInterpretation(
            intent = HatchyIntent.INCUBATION_STATUS,
            query = "how are my hatches",
            confidence = 1.0
        )
        val outcomeA = resolver.resolve(interpretationA, context) as ResolverOutcome.Resolved
        
        // Scenario B: Temperature Query
        every { runBlocking { incubationQueryService.resolveIncubationStatusQuery(null, null, context) } } returns QueryResolutionResult(
            data = mapOf("temp" to 99.5),
            summary = "Temp is fine",
            confidence = 0.9,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(matchScore = 0.9, matchedTopic = "Temperature")
        )
        
        val interpretationB = makeInterpretation(
            intent = HatchyIntent.INCUBATION_STATUS,
            query = "what is the temp",
            confidence = 0.9,
            entities = listOf(HatchyEntity(EntityType.INCUBATION_TOPIC, "Temperature", "temp"))
        )
        val outcomeB = resolver.resolve(interpretationB, context) as ResolverOutcome.Resolved
        
        assertNotEquals("Distinct queries should return distinct summaries", outcomeA.answer.text, outcomeB.answer.text)
        assertEquals("TEMPERATURE", (outcomeB.answer.debugMetadata?.get("matchedTopic") as String).uppercase())
    }

    @Test
    fun `behavioral contract - nursery resolver must handle split status topics`() = runBlocking {
        val resolver = registry.getResolver(HatchyIntent.NURSERY_STATUS) as NurseryStatusResolver
        val context = createTestContext()
        
        every { runBlocking { nurseryQueryService.resolveNurseryStatusQuery(null, any(), context) } } returns QueryResolutionResult(
            data = mapOf("count" to 42),
            summary = "42 chicks in nursery",
            confidence = 1.0,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(matchScore = 1.0, matchedTopic = "ACTIVE_COUNT")
        )
        
        val interpretation = makeInterpretation(
            intent = HatchyIntent.NURSERY_STATUS,
            query = "how many chicks",
            confidence = 1.0,
            entities = listOf(HatchyEntity(EntityType.NURSERY_STATUS_TOPIC, "ActiveCount", "how many"))
        )
        val outcome = resolver.resolve(interpretation, context) as ResolverOutcome.Resolved
        
        assertEquals("42 chicks in nursery", outcome.answer.text)
        assertEquals("ACTIVE_COUNT", outcome.answer.debugMetadata?.get("matchedTopic"))
    }

    @Test
    fun `behavioral contract - finance help must be context-aware`() = runBlocking {
        val resolver = registry.getResolver(HatchyIntent.FINANCE_HELP) as FinanceHelpResolver
        val context = createTestContext()
        
        every { runBlocking { financeQueryService.getHelp(null, context) } } returns KnowledgeMatchResult(
            content = "Tap plus to log expense",
            confidence = 0.9,
            source = AnswerSource.APP_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(matchScore = 0.9, matchedTopic = "LOG_EXPENSE")
        )
        
        val interpretation = makeInterpretation(
            intent = HatchyIntent.FINANCE_HELP,
            query = "how to log expense",
            confidence = 0.9,
            entities = listOf(HatchyEntity(EntityType.FINANCE_HELP_TOPIC, "LogExpense", "how to log"))
        )
        val outcome = resolver.resolve(interpretation, context) as ResolverOutcome.Resolved
        
        assertTrue(outcome.answer.text.contains("log expense"))
        assertEquals("LOG_EXPENSE", outcome.answer.debugMetadata?.get("matchedTopic"))
    }

    @Test
    fun `low-evidence safety test - weak input must degrade confidence`() = runBlocking {
        val resolver = registry.getResolver(HatchyIntent.INCUBATION_STATUS) as IncubationStatusResolver
        val context = createTestContext()
        
        // Strong service result but WEAK intent/entity understanding
        every { runBlocking { incubationQueryService.resolveIncubationStatusQuery(any(), any(), any()) } } returns QueryResolutionResult(
            data = mapOf("status" to "ok"),
            summary = "Perfect data",
            confidence = 1.0,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(matchScore = 1.0)
        )
        
        val interpretation = makeInterpretation(
            intent = HatchyIntent.INCUBATION_STATUS,
            query = "something vague",
            confidence = 0.3
        ) // WEAK intent
        val outcome = resolver.resolve(interpretation, context) as ResolverOutcome.Resolved
        
        // Final confidence MUST be capped due to weak intent/entities
        assertTrue(
            "Final confidence must be MEDIUM or lower if intent/entity confidence is low (< 0.5)",
            outcome.answer.confidence == AnswerConfidence.MEDIUM || outcome.answer.confidence == AnswerConfidence.LOW
        )
    }

    @Test
    fun `fallback resolver compliance - truthfulness and safe wording`() = runBlocking {
        val resolver = registry.getResolver(HatchyIntent.OTHER) as FallbackResolver
        val interpretation = makeInterpretation(
            intent = HatchyIntent.OTHER,
            query = "nonsensical query",
            confidence = 0.1
        )
        val outcome = resolver.resolve(interpretation, createTestContext()) as ResolverOutcome.Resolved
        
        assertEquals(AnswerConfidence.VERY_LOW, outcome.answer.confidence)
        assertTrue("Fallback answer should be professional and list domains", outcome.answer.text.contains("breeding"))
        assertEquals("SYSTEM_FALLBACK", outcome.answer.debugMetadata?.get("outcome"))
    }

    private fun createTestContext() = HatchyContextSnapshot(
        currentModule = "HOME",
        selectedSpecies = null,
        tier = com.example.hatchtracker.data.models.SubscriptionTier.FREE,
        isAdminOrDeveloper = false,
        recentBreedsMentioned = emptyList(),
        recentTraitsMentioned = emptyList(),
        lastResult = null,
        hasUserDataContext = false
    )

    private fun makeInterpretation(
        intent: HatchyIntent,
        query: String,
        confidence: Double = 0.9,
        entities: List<HatchyEntity> = emptyList()
    ) = QueryInterpretation(
        rawQuery = query,
        questionMode = QuestionModeResult(
            primaryMode = QuestionMode.UNKNOWN,
            secondaryMode = null,
            modeConfidence = 0.0,
            appAnchorScore = 0.0,
            realWorldAnchorScore = 0.0,
            userDataAnchorScore = 0.0
        ),
        entities = entities,
        intent = intent,
        topicResult = TopicInferenceResult(
            primaryTopic = null,
            secondaryTopic = null,
            topicScores = emptyMap(),
            confidence = 0.0
        ),
        inferredGoals = emptyList(),
        confidence = confidence
    )
}
