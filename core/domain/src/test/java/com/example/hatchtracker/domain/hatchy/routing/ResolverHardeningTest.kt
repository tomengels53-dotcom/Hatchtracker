package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.CrossbreedingSimulationService
import com.example.hatchtracker.domain.hatchy.routing.services.EvidenceMetadata
import com.example.hatchtracker.domain.hatchy.routing.services.FinanceQueryService
import com.example.hatchtracker.domain.hatchy.routing.services.IncubationKnowledgeService
import com.example.hatchtracker.domain.hatchy.routing.services.IncubationQueryService
import com.example.hatchtracker.domain.hatchy.routing.services.KnowledgeMatchResult
import com.example.hatchtracker.domain.hatchy.routing.services.NurseryQueryService
import com.example.hatchtracker.domain.hatchy.routing.services.QueryResolutionResult
import com.example.hatchtracker.domain.hatchy.routing.services.RecommendationResult
import com.example.hatchtracker.domain.hatchy.routing.services.PoultryKnowledgeService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResolverHardeningTest {

    private lateinit var responseComposer: HatchyResponseComposer

    @Before
    fun setup() {
        responseComposer = mockk(relaxed = true)
    }

    @Test
    fun `UserDataAwareResolver routes incubating batch query to incubation answer`() = runBlocking {
        val nurseryService = mockk<NurseryQueryService>()
        val incubationService = mockk<IncubationQueryService>()

        coEvery { incubationService.resolveIncubationStatusQuery(any(), any(), any()) } returns QueryResolutionResult(
            data = emptyMap(),
            summary = "Incubation batch looks stable.",
            confidence = 0.8,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(matchedTopic = "STATUS")
        )

        val resolver = UserDataAwareResolver(nurseryService, incubationService, responseComposer)

        val interpretation = QueryInterpretation(
            rawQuery = "i have an incubating batch",
            questionMode = userDataMode(),
            entities = emptyList(),
            intent = HatchyIntent.USER_DATA_QUERY,
            topicResult = TopicInferenceResult(null, null, emptyMap(), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.9
        )

        val outcome = resolver.resolve(interpretation, testContext())

        assertTrue(outcome is ResolverOutcome.Resolved)
        outcome as ResolverOutcome.Resolved
        assertEquals(AnswerType.INCUBATION, outcome.answer.type)
        assertEquals(AnswerSource.USER_DATA, outcome.answer.source)
    }

    @Test
    fun `PoultryKnowledgeResolver enforces AnswerSource POULTRY_KNOWLEDGE_BASE`() = runBlocking {
        val poultryService = mockk<PoultryKnowledgeService>()

        coEvery { poultryService.findMatch(any(), any(), any(), any()) } returns KnowledgeMatchResult(
            content = "Chicken wings",
            confidence = 0.9,
            source = AnswerSource.FALLBACK,
            evidence = EvidenceMetadata(matchedTopic = "GENERAL")
        )

        val resolver = PoultryKnowledgeResolver(poultryService, responseComposer)

        val interpretation = QueryInterpretation(
            rawQuery = "tell me about chickens",
            questionMode = realWorldMode(),
            entities = emptyList(),
            intent = HatchyIntent.GENERAL_POULTRY,
            topicResult = TopicInferenceResult(null, null, emptyMap(), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.9
        )

        val outcome = resolver.resolve(interpretation, testContext())

        assertTrue(outcome is ResolverOutcome.Resolved)
        outcome as ResolverOutcome.Resolved
        assertEquals(AnswerSource.POULTRY_KNOWLEDGE_BASE, outcome.answer.source)
    }

    @Test
    fun `IncubationGuidanceResolver returns InsufficientEvidence for weak match`() = runBlocking {
        val incubationService = mockk<IncubationKnowledgeService>()

        coEvery { incubationService.findMatch(any(), any(), any(), any()) } returns null

        val resolver = IncubationGuidanceResolver(incubationService, responseComposer)

        val interpretation = QueryInterpretation(
            rawQuery = "set up",
            questionMode = realWorldMode(),
            entities = emptyList(),
            intent = HatchyIntent.INCUBATION_GUIDANCE,
            topicResult = TopicInferenceResult(null, null, emptyMap(), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.9
        )

        val outcome = resolver.resolve(interpretation, testContext())

        assertTrue(outcome is ResolverOutcome.InsufficientEvidence)
    }

    @Test
    fun `FinanceHelpResolver returns resolved answer from non-null service contract`() = runBlocking {
        val financeService = mockk<FinanceQueryService>()

        coEvery { financeService.getHelp(any(), any()) } returns KnowledgeMatchResult(
            content = "Open the Finance module to log an expense.",
            confidence = 1.0,
            source = AnswerSource.APP_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(matchedTopic = "FINANCE_HELP")
        )

        val resolver = FinanceHelpResolver(financeService, responseComposer)

        val interpretation = QueryInterpretation(
            rawQuery = "how much did i",
            questionMode = appWorkflowMode(),
            entities = emptyList(),
            intent = HatchyIntent.FINANCE_HELP,
            topicResult = TopicInferenceResult(null, null, emptyMap(), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.9
        )

        val outcome = resolver.resolve(interpretation, testContext())

        assertTrue(outcome is ResolverOutcome.Resolved)
        outcome as ResolverOutcome.Resolved
        assertEquals(AnswerType.FINANCE, outcome.answer.type)
        assertEquals(AnswerSource.APP_KNOWLEDGE_BASE, outcome.answer.source)
    }

    @Test
    fun `BreedingSimulationResolver returns InsufficientEvidence instead of crash on empty reasoning`() = runBlocking {
        val breedingService = mockk<CrossbreedingSimulationService>()

        coEvery { breedingService.simulate(any(), any(), any()) } returns RecommendationResult(
            candidates = emptyList(),
            reasoning = "",
            confidence = 0.0,
            source = AnswerSource.BREEDING_ENGINE,
            evidence = EvidenceMetadata(matchedTopic = "CROSSBREEDING_SIMULATION")
        )

        val resolver = BreedingSimulationResolver(breedingService, responseComposer)

        val interpretation = QueryInterpretation(
            rawQuery = "cross silkie and leghorn",
            questionMode = realWorldMode(),
            entities = listOf(
                HatchyEntity(EntityType.BREED, "silkie", "silkie", 1.0),
                HatchyEntity(EntityType.BREED, "leghorn", "leghorn", 1.0)
            ),
            intent = HatchyIntent.CROSSBREED_OUTCOME,
            topicResult = TopicInferenceResult(KnowledgeTopic.CROSSBREED_RECOMMENDATION, null, emptyMap(), 1.0),
            inferredGoals = emptyList(),
            confidence = 0.9
        )

        val outcome = resolver.resolve(interpretation, testContext())

        assertTrue(outcome is ResolverOutcome.InsufficientEvidence)
    }

    private fun testContext() = HatchyContextSnapshot(
        currentModule = "HOME",
        selectedSpecies = null,
        tier = SubscriptionTier.FREE,
        isAdminOrDeveloper = false,
        recentBreedsMentioned = emptyList(),
        recentTraitsMentioned = emptyList(),
        lastResult = null,
        hasUserDataContext = false
    )

    private fun realWorldMode() = QuestionModeResult(
        primaryMode = QuestionMode.REAL_WORLD_GUIDANCE,
        secondaryMode = null,
        modeConfidence = 1.0,
        appAnchorScore = 0.0,
        realWorldAnchorScore = 1.0,
        userDataAnchorScore = 0.0
    )

    private fun appWorkflowMode() = QuestionModeResult(
        primaryMode = QuestionMode.APP_WORKFLOW,
        secondaryMode = null,
        modeConfidence = 1.0,
        appAnchorScore = 1.0,
        realWorldAnchorScore = 0.0,
        userDataAnchorScore = 0.0
    )

    private fun userDataMode() = QuestionModeResult(
        primaryMode = QuestionMode.USER_DATA_STATUS,
        secondaryMode = null,
        modeConfidence = 1.0,
        appAnchorScore = 0.0,
        realWorldAnchorScore = 0.0,
        userDataAnchorScore = 1.0
    )
}
