package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.routing.*
import com.example.hatchtracker.domain.hatchy.routing.services.*
import com.example.hatchtracker.data.models.SubscriptionTier
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

class ResolverSelectionTest {

    private val classifier = mockk<HatchyIntentClassifier>()
    private val questionModeClassifier = mockk<QuestionModeClassifier>()
    private val entityExtractor = mockk<HatchyEntityExtractor>()
    private val contextBuilder = mockk<HatchyContextSnapshotBuilder>()
    private val responseComposer = mockk<HatchyResponseComposer>()
    private val confidenceEvaluator = mockk<HatchyConfidenceEvaluator>()
    private val topicEngine = TopicInferenceEngine(LexiconRegistry())
    private val pacingPolicy = ResponsePacingPolicy()
    private val poultryService = mockk<PoultryKnowledgeService>(relaxed = true)
    private val incubationKnowledgeService = mockk<IncubationKnowledgeService>(relaxed = true)
    private val incubationQueryService = mockk<IncubationQueryService>(relaxed = true)
    private val nurseryKnowledgeService = mockk<NurseryKnowledgeService>(relaxed = true)
    private val nurseryQueryService = mockk<NurseryQueryService>(relaxed = true)
    private val financeQueryService = mockk<FinanceQueryService>(relaxed = true)
    private val equipmentQueryService = mockk<EquipmentQueryService>(relaxed = true)
    private val breedingGuidanceService = mockk<BreedingGuidanceKnowledgeService>(relaxed = true)
    private val breedingSimulationService = mockk<CrossbreedingSimulationService>(relaxed = true)
    private val flockBreedingAdvisorService = mockk<FlockBreedingAdvisorService>(relaxed = true)
    private val breedRepository = mockk<IBreedStandardRepository>(relaxed = true)

    private lateinit var registry: HatchyResolverRegistry

    private lateinit var orchestrator: HatchyOrchestrator

    @Before
    fun setup() {
        registry = createRegistry()
        orchestrator = HatchyOrchestrator(
            classifier,
            questionModeClassifier,
            entityExtractor,
            contextBuilder,
            registry,
            responseComposer,
            confidenceEvaluator,
            topicEngine,
            pacingPolicy
        )

        // Standard mock behavior for composer
        every { responseComposer.compose(any(), any(), any()) } answers { it.invocation.args[0] as HatchyAnswer }
    }

    @Test
    fun `test incubation status query selection`() = runBlocking {
        val query = "how is my incubation batch doing"
        
        // Mock interpretation signals
        every { classifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.INCUBATION_STATUS, confidence = 0.9)
        every { questionModeClassifier.classify(any()) } returns questionModeResult(
            primaryMode = QuestionMode.USER_DATA_STATUS,
            appAnchorScore = 0.1,
            realWorldAnchorScore = 0.1,
            userDataAnchorScore = 0.8
        )
        every { entityExtractor.extract(any()) } returns listOf(
            HatchyEntity(EntityType.USER_DATA_REF, "active_batch", "my incubation batch")
        )
        every { contextBuilder.build(any(), any()) } returns testSnapshot()
        every {
            runBlocking { incubationQueryService.resolveIncubationStatusQuery(null, null, any()) }
        } returns QueryResolutionResult(
            data = mapOf("status" to "stable"),
            summary = "Incubation batch looks stable.",
            confidence = 0.9,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(matchScore = 1.0, matchedTopic = "STATUS")
        )

        val answer = orchestrator.processQuery(query, mockk())
        
        // Should be handled by IncubationStatusResolver
        assertEquals(AnswerType.INCUBATION, answer.type)
        assertEquals(AnswerSource.USER_DATA, answer.source)
    }

    @Test
    fun `test breeding guidance selection with priority tie-break`() = runBlocking {
        val query = "breeding strategy for leghorns"
        
        // Mock interpretation signals
        every { classifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.BREEDING_GUIDANCE, confidence = 0.9)
        every { questionModeClassifier.classify(any()) } returns questionModeResult(
            primaryMode = QuestionMode.REAL_WORLD_GUIDANCE,
            appAnchorScore = 0.1,
            realWorldAnchorScore = 0.9,
            userDataAnchorScore = 0.1
        )
        every { entityExtractor.extract(any()) } returns listOf(HatchyEntity(EntityType.BREED, "Leghorn", "leghorns"))
        every { contextBuilder.build(any(), any()) } returns testSnapshot()
        every {
            runBlocking { breedingGuidanceService.findMatch(any(), any(), any(), any()) }
        } returns KnowledgeMatchResult(
            content = "Focus on selecting birds with strong production traits.",
            confidence = 0.9,
            source = AnswerSource.APP_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(matchScore = 1.0, matchedTopic = "BREEDING_STRATEGY")
        )

        val answer = orchestrator.processQuery(query, mockk())
        
        // Should be handled by BreedingGuidanceResolver (priority OVER Knowledge)
        assertEquals(AnswerType.GUIDANCE, answer.type)
    }

    @Test
    fun `test fallback selection when no resolver scores high`() = runBlocking {
        val query = "what is the meaning of life"
        
        every { classifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.OTHER, confidence = 0.1)
        every { questionModeClassifier.classify(any()) } returns questionModeResult(
            primaryMode = QuestionMode.UNKNOWN,
            appAnchorScore = 0.1,
            realWorldAnchorScore = 0.1,
            userDataAnchorScore = 0.1,
            modeConfidence = 0.0
        )
        every { entityExtractor.extract(any()) } returns emptyList()
        every { contextBuilder.build(any(), any()) } returns testSnapshot()

        val answer = orchestrator.processQuery(query, mockk())
        
        assertEquals(AnswerType.FALLBACK, answer.type)
        assertEquals(AnswerSource.FALLBACK, answer.source)
    }

    @Test
    fun `test budget summary selection`() = runBlocking {
        val query = "total spending this month"
        
        every { classifier.classify(any(), any()) } returns HatchyIntentResult(HatchyIntent.FINANCE_SUMMARY, confidence = 0.9)
        every { questionModeClassifier.classify(any()) } returns questionModeResult(
            primaryMode = QuestionMode.USER_DATA_STATUS,
            appAnchorScore = 0.9,
            realWorldAnchorScore = 0.1,
            userDataAnchorScore = 0.9
        )
        every { entityExtractor.extract(any()) } returns emptyList()
        every { contextBuilder.build(any(), any()) } returns testSnapshot()
        every {
            runBlocking { financeQueryService.resolveFinanceSummaryQuery(null, null, any()) }
        } returns QueryResolutionResult(
            data = mapOf("total" to 125.0),
            summary = "Total spending this month is 125.0.",
            confidence = 0.9,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(matchScore = 1.0, matchedTopic = "TOTAL_SPEND")
        )

        val answer = orchestrator.processQuery(query, mockk())
        
        assertEquals(AnswerType.FINANCE, answer.type)
        assertEquals(AnswerSource.USER_DATA, answer.source)
    }

    private fun createRegistry(): HatchyResolverRegistry {
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

        return HatchyResolverRegistry(
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

    private fun questionModeResult(
        primaryMode: QuestionMode,
        appAnchorScore: Double,
        realWorldAnchorScore: Double,
        userDataAnchorScore: Double,
        modeConfidence: Double = 1.0,
        secondaryMode: QuestionMode? = null
    ) = QuestionModeResult(
        primaryMode = primaryMode,
        secondaryMode = secondaryMode,
        modeConfidence = modeConfidence,
        appAnchorScore = appAnchorScore,
        realWorldAnchorScore = realWorldAnchorScore,
        userDataAnchorScore = userDataAnchorScore
    )

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
