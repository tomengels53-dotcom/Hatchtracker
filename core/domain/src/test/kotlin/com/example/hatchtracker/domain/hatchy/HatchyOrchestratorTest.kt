package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.model.HatchyModule
import com.example.hatchtracker.domain.hatchy.routing.AnswerType
import com.example.hatchtracker.domain.hatchy.routing.AppNavigationResolver
import com.example.hatchtracker.domain.hatchy.routing.BreedKnowledgeResolver
import com.example.hatchtracker.domain.hatchy.routing.BreedingGuidanceResolver
import com.example.hatchtracker.domain.hatchy.routing.BreedingSimulationResolver
import com.example.hatchtracker.domain.hatchy.routing.EquipmentHelpResolver
import com.example.hatchtracker.domain.hatchy.routing.EquipmentStatusResolver
import com.example.hatchtracker.domain.hatchy.routing.FallbackResolver
import com.example.hatchtracker.domain.hatchy.routing.FinanceHelpResolver
import com.example.hatchtracker.domain.hatchy.routing.FinanceSummaryResolver
import com.example.hatchtracker.domain.hatchy.routing.FlockBreedingAdvisorResolver
import com.example.hatchtracker.domain.hatchy.routing.HatchyContextSnapshotBuilder
import com.example.hatchtracker.domain.hatchy.routing.HatchyEntityExtractor
import com.example.hatchtracker.domain.hatchy.routing.HatchyIntentClassifier
import com.example.hatchtracker.domain.hatchy.routing.HatchyResolverRegistry
import com.example.hatchtracker.domain.hatchy.routing.IncubationGuidanceResolver
import com.example.hatchtracker.domain.hatchy.routing.IncubationStatusResolver
import com.example.hatchtracker.domain.hatchy.routing.KeywordBundleRepository
import com.example.hatchtracker.domain.hatchy.routing.NurseryGuidanceResolver
import com.example.hatchtracker.domain.hatchy.routing.NurseryStatusResolver
import com.example.hatchtracker.domain.hatchy.routing.PoultryKnowledgeResolver
import com.example.hatchtracker.domain.hatchy.routing.QuestionModeClassifier
import com.example.hatchtracker.domain.hatchy.routing.ResponsePacingPolicy
import com.example.hatchtracker.domain.hatchy.routing.TopicInferenceEngine
import com.example.hatchtracker.domain.hatchy.routing.LexiconRegistry
import com.example.hatchtracker.domain.hatchy.routing.UserDataAwareResolver
import com.example.hatchtracker.domain.hatchy.routing.services.BreedingGuidanceKnowledgeService
import com.example.hatchtracker.domain.hatchy.routing.services.CrossbreedingSimulationService
import com.example.hatchtracker.domain.hatchy.routing.services.EquipmentQueryService
import com.example.hatchtracker.domain.hatchy.routing.services.FlockBreedingAdvisorService
import com.example.hatchtracker.domain.hatchy.routing.services.FinanceQueryService
import com.example.hatchtracker.domain.hatchy.routing.services.IncubationKnowledgeService
import com.example.hatchtracker.domain.hatchy.routing.services.IncubationQueryService
import com.example.hatchtracker.domain.hatchy.routing.services.NurseryKnowledgeService
import com.example.hatchtracker.domain.hatchy.routing.services.NurseryQueryService
import com.example.hatchtracker.domain.hatchy.routing.services.PoultryKnowledgeService
import com.example.hatchtracker.domain.hatchy.routing.services.TemperatureFormatter
import com.example.hatchtracker.model.BreedStandard
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

class HatchyOrchestratorTest {

    private val breedRepository = mockk<com.example.hatchtracker.domain.hatchy.routing.IBreedStandardRepository>()
    private val bundleRepository = KeywordBundleRepository()
    private val classifier = HatchyIntentClassifier(bundleRepository)
    private val questionModeClassifier = QuestionModeClassifier()
    private val entityExtractor = HatchyEntityExtractor(breedRepository)
    private val contextBuilder = HatchyContextSnapshotBuilder()
    private val responseComposer = HatchyResponseComposer()
    private val confidenceEvaluator = HatchyConfidenceEvaluator()
    private val topicInferenceEngine = TopicInferenceEngine(LexiconRegistry())
    private val pacingPolicy = ResponsePacingPolicy()
    private val tempFormatter = TemperatureFormatter()

    private val breedingGuidanceService = BreedingGuidanceKnowledgeService()
    private val breedingSimulationService = CrossbreedingSimulationService()
    private val flockBreedingAdvisorService = FlockBreedingAdvisorService()
    private val poultryService = PoultryKnowledgeService(tempFormatter)
    private val incubationQueryService = IncubationQueryService(mockk(relaxed = true))
    private val incubationKnowledgeService = IncubationKnowledgeService(tempFormatter)
    private val nurseryKnowledgeService = NurseryKnowledgeService(tempFormatter)
    private val nurseryQueryService = NurseryQueryService(mockk(relaxed = true))
    private val financeQueryService = FinanceQueryService()
    private val equipmentQueryService = EquipmentQueryService()

    private val appNavResolver = AppNavigationResolver(responseComposer)
    private val breedKnowledgeResolver = BreedKnowledgeResolver(breedRepository, responseComposer)
    private val breedingSimulationResolver = BreedingSimulationResolver(breedingSimulationService, responseComposer)
    private val breedingGuidanceResolver = BreedingGuidanceResolver(breedingGuidanceService, responseComposer)
    private val poultryKnowledgeResolver = PoultryKnowledgeResolver(poultryService, responseComposer)
    private val incubationGuidanceResolver = IncubationGuidanceResolver(incubationKnowledgeService, responseComposer)
    private val incubationStatusResolver = IncubationStatusResolver(incubationQueryService, responseComposer)
    private val nurseryGuidanceResolver = NurseryGuidanceResolver(nurseryKnowledgeService, responseComposer)
    private val nurseryStatusResolver = NurseryStatusResolver(nurseryQueryService, responseComposer)
    private val financeHelpResolver = FinanceHelpResolver(financeQueryService, responseComposer)
    private val financeSummaryResolver = FinanceSummaryResolver(financeQueryService, responseComposer)
    private val equipmentHelpResolver = EquipmentHelpResolver(equipmentQueryService, responseComposer)
    private val equipmentStatusResolver = EquipmentStatusResolver(equipmentQueryService, responseComposer)
    private val flockBreedingAdvisorResolver = FlockBreedingAdvisorResolver(flockBreedingAdvisorService, responseComposer)
    private val userDataAwareResolver = UserDataAwareResolver(nurseryQueryService, incubationQueryService, responseComposer)
    private val fallbackResolver = FallbackResolver(responseComposer)

    private val registry = HatchyResolverRegistry(
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

    private lateinit var orchestrator: HatchyOrchestrator

    @Before
    fun setup() {
        orchestrator = HatchyOrchestrator(
            classifier,
            questionModeClassifier,
            entityExtractor,
            contextBuilder,
            registry,
            responseComposer,
            confidenceEvaluator,
            topicInferenceEngine,
            pacingPolicy
        )

        every { breedRepository.getAllBreeds() } returns listOf(
            BreedStandard(id = "jg", name = "Jersey Giant", species = "Chicken"),
            BreedStandard(id = "rir", name = "Rhode Island Red", species = "Chicken")
        )
        every { breedRepository.getBreedById("jg") } returns BreedStandard(id = "jg", name = "Jersey Giant", species = "Chicken")
        every { breedRepository.getBreedById("rir") } returns BreedStandard(id = "rir", name = "Rhode Island Red", species = "Chicken")
    }

    @Test
    fun `test regression 1 - breed info egg production`() = runBlocking {
        val query = "Which chicken breed would be the best egg layer?"
        val answer = orchestrator.processQuery(query, mockContext())

        assertEquals(AnswerType.BREED_INFO, answer.type)
        assertTrue(answer.text.contains("White Leghorn") || answer.text.contains("ISA Brown"))
    }

    @Test
    fun `test regression 2 - crossbreeding outcome`() = runBlocking {
        val query = "What would happen if I cross breed a Jersey Giant with a Rhode Island Red?"
        val answer = orchestrator.processQuery(query, mockContext())

        assertEquals(AnswerType.CROSSBREEDING, answer.type)
        val hasFirstBreed = answer.text.contains("Jersey Giant", ignoreCase = true) ||
            answer.text.contains("jg", ignoreCase = true)
        val hasSecondBreed = answer.text.contains("Rhode Island Red", ignoreCase = true) ||
            answer.text.contains("rir", ignoreCase = true)
        assertTrue(hasFirstBreed && hasSecondBreed)
    }

    @Test
    fun `test regression 5 - app navigation`() = runBlocking {
        val query = "How do I start a new incubation?"
        val answer = orchestrator.processQuery(query, mockContext())

        assertEquals(AnswerType.NAVIGATION, answer.type)
        assertTrue(answer.text.contains("Incubation"))
    }

    @Test
    fun `test regression 8 - poultry knowledge`() = runBlocking {
        val query = "Why are my chickens not laying eggs?"
        val answer = orchestrator.processQuery(query, mockContext())

        assertEquals(AnswerType.POULTRY_KNOWLEDGE, answer.type)
        assertTrue(answer.text.contains("drop in laying"))
    }

    private fun mockContext() = HatchyContext(
        currentModule = HatchyModule.HOME,
        tier = SubscriptionTier.FREE,
        isAdminOrDeveloper = false
    )
}
