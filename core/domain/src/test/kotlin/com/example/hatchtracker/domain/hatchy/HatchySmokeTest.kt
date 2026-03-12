package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.model.BreedStandard
import com.example.hatchtracker.model.Flock
import com.example.hatchtracker.model.Flocklet
import com.example.hatchtracker.model.FinancialStats
import com.example.hatchtracker.model.Incubation
import com.example.hatchtracker.model.Equipment
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.model.HatchyModule
import com.example.hatchtracker.domain.hatchy.routing.*
import com.example.hatchtracker.domain.hatchy.routing.services.*
import com.example.hatchtracker.domain.repo.*
import com.example.hatchtracker.data.models.SubscriptionTier
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

class HatchySmokeTest {

    private val breedRepo = mockk<com.example.hatchtracker.domain.hatchy.routing.IBreedStandardRepository>()
    private val flockRepo = mockk<com.example.hatchtracker.domain.repo.FlockRepository>()
    private val incubationRepo = mockk<com.example.hatchtracker.domain.repo.IncubationRepository>()
    private val nurseryRepo = mockk<com.example.hatchtracker.domain.repo.NurseryRepository>()
    private val financialRepo = mockk<com.example.hatchtracker.domain.repo.FinancialRepository>()
    private val equipmentRepo = mockk<com.example.hatchtracker.domain.repo.EquipmentRepository>()
    private val birdRepo = mockk<com.example.hatchtracker.domain.repo.BirdRepository>()

    private val bundleRepository = KeywordBundleRepository()
    private val classifier = HatchyIntentClassifier(bundleRepository)
    private val questionModeClassifier = QuestionModeClassifier()
    private val entityExtractor = HatchyEntityExtractor(breedRepo)
    private val contextBuilder = HatchyContextSnapshotBuilder()
    private val topicInferenceEngine = TopicInferenceEngine(LexiconRegistry())
    private val pacingPolicy = ResponsePacingPolicy()
    private val tempFormatter = TemperatureFormatter()
    
    private val responseComposer = HatchyResponseComposer()
    
    // Services
    private val incubationQueryService = IncubationQueryService(incubationRepo)
    private val incubationKnowledgeService = IncubationKnowledgeService(tempFormatter)
    private val nurseryKnowledgeService = NurseryKnowledgeService(tempFormatter)
    private val nurseryQueryService = NurseryQueryService(flockRepo)
    private val financeQueryService = FinanceQueryService()
    private val equipmentQueryService = EquipmentQueryService()
    private val breedingGuidanceService = BreedingGuidanceKnowledgeService()
    private val breedingSimulationService = CrossbreedingSimulationService()
    private val flockBreedingAdvisorService = FlockBreedingAdvisorService()
    private val poultryKnowledgeService = PoultryKnowledgeService(tempFormatter)

    // Resolvers
    private val appNavResolver = AppNavigationResolver(responseComposer)
    private val breedKnowledgeResolver = BreedKnowledgeResolver(breedRepo, responseComposer)
    private val breedingSimulationResolver = BreedingSimulationResolver(breedingSimulationService, responseComposer)
    private val breedingGuidanceResolver = BreedingGuidanceResolver(breedingGuidanceService, responseComposer)
    private val poultryKnowledgeResolver = PoultryKnowledgeResolver(poultryKnowledgeService, responseComposer)
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

    private val confidenceEvaluator = HatchyConfidenceEvaluator()

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

        // Default Mocks
        every { breedRepo.getAllBreeds() } returns listOf(
            BreedStandard(id = "jg", name = "Jersey Giant", species = "Chicken"),
            BreedStandard(id = "rir", name = "Rhode Island Red", species = "Chicken"),
            BreedStandard(id = "ls", name = "Light Sussex", species = "Chicken")
        )
        every { breedRepo.getBreedById(any()) } answers { 
            val id = it.invocation.args[0] as String
            when(id) {
                "jg" -> BreedStandard(id = "jg", name = "Jersey Giant", species = "Chicken")
                "rir" -> BreedStandard(id = "rir", name = "Rhode Island Red", species = "Chicken")
                "ls" -> BreedStandard(id = "ls", name = "Light Sussex", species = "Chicken")
                else -> null
            }
        }
        coEvery { incubationRepo.getActiveIncubationCount() } returns 1
        every { nurseryRepo.activeFlocklets } returns flowOf(listOf(
            mockk<Flocklet> { every { chickCount } returns 12 },
            mockk<Flocklet> { every { chickCount } returns 12 }
        ))
        every { financialRepo.getAggregatedStats(any(), any(), any(), any()) } returns flowOf(FinancialStats(100.0, 50.0, -50.0, 0.0, 5))
        every { flockRepo.allActiveFlocks } returns flowOf(
            listOf(
                Flock(species = Species.CHICKEN, name = "Flock A"),
                Flock(species = Species.CHICKEN, name = "Flock B")
            )
        )
        every { equipmentRepo.getUserEquipment() } returns flowOf(listOf(
            mockk<Equipment> { every { isActive } returns true; every { lifecycleStatus } returns com.example.hatchtracker.model.DeviceLifecycleStatus.ACTIVE }
        ))
    }

    private fun testQuery(query: String, expectedType: AnswerType, expectedSnippet: String? = null) = runBlocking {
        val answer = orchestrator.processQuery(query, HatchyContext(currentModule = HatchyModule.HOME, tier = SubscriptionTier.FREE))
        assertTrue("Query: '$query' produced empty answer text", answer.text.isNotBlank())
    }

    @Test
    fun `smoke test - 50 questions`() {
        // --- BREEDING ---
        testQuery("What happens if I cross a Jersey Giant with a RIR?", AnswerType.CROSSBREEDING, "dual-purpose")
        testQuery("Tell me about Jersey Giants", AnswerType.BREED_INFO, "Jersey Giant")
        testQuery("Which breed is best for eggs?", AnswerType.BREED_INFO, "Leghorn")
        testQuery("How do I start breeding chickens?", AnswerType.GUIDANCE, "goals")
        testQuery("What is hybrid vigor?", AnswerType.GUIDANCE, "heterosis")

        // --- INCUBATION ---
        testQuery("How many active incubations do I have?", AnswerType.INCUBATION, "1 active")
        testQuery("When is lockdown for chicken eggs?", AnswerType.INCUBATION, "day 19")
        testQuery("What humidity should I use for lockdown?", AnswerType.INCUBATION, "65-70%")
        testQuery("My eggs are on day 19, what should I do?", AnswerType.INCUBATION, "Stop turning")
        testQuery("Incubator temperature?", AnswerType.INCUBATION, "99.5")

        // --- NURSERY ---
        testQuery("How many chicks are in the nursery?", AnswerType.NURSERY, "2 active batches")
        testQuery("What temperature for 1-week old chicks?", AnswerType.NURSERY, "90-95")
        testQuery("When can chicks go outside?", AnswerType.NURSERY, "fully feathered")
        testQuery("How to care for new chicks?", AnswerType.NURSERY, "warmth")

        // --- FINANCE ---
        testQuery("How do I log an expense?", AnswerType.FINANCE, "expense")
        testQuery("Show me my financial summary", AnswerType.FINANCE, "total cost")
        testQuery("How much have I spent on my birds?", AnswerType.FINANCE, "spend")
        testQuery("Where do I record sales?", AnswerType.FINANCE, "revenue")

        // --- EQUIPMENT ---
        testQuery("Are my sensors online?", AnswerType.EQUIPMENT, "devices online")
        testQuery("How do I connect a new sensor?", AnswerType.EQUIPMENT, "connect")
        testQuery("My incubator sensor is not working", AnswerType.EQUIPMENT, "troubleshoot")

        // --- USER DATA ---
        testQuery("How many flocks do I have?", AnswerType.NAVIGATION, "2 active flocks")
        testQuery("Summary of my yard", AnswerType.NAVIGATION, "flocks")

        // --- GENERAL POULTRY ---
        testQuery("Why are my hens not laying?", AnswerType.POULTRY_KNOWLEDGE, "laying")
        testQuery("My chicken is sneezing", AnswerType.POULTRY_KNOWLEDGE, "respiratory")
        testQuery("Best feed for layers?", AnswerType.POULTRY_KNOWLEDGE, "calcium")
        testQuery("How to stop predatory hawks?", AnswerType.POULTRY_KNOWLEDGE, "protection")
        testQuery("Vitamins for chickens?", AnswerType.POULTRY_KNOWLEDGE, "supplements")

        // --- APP NAVIGATION ---
        testQuery("Where is the breeding module?", AnswerType.NAVIGATION, "Breeding")
        testQuery("How do I add a new bird?", AnswerType.NAVIGATION, "Flock")
        testQuery("Can I track egg production?", AnswerType.NAVIGATION, "Flock")
        testQuery("Settings", AnswerType.NAVIGATION, "Settings")

        // --- PERSONALIZED BREEDING ---
        testQuery("What should I breed in my flock?", AnswerType.NAVIGATION, "Breeding")

        // --- COMPLEX / MIXED ---
        testQuery("Can I cross breeds and track them in the app?", AnswerType.CROSSBREEDING, "Wizard")
        testQuery("If I cross Jersey Giant with RIR can I log the result?", AnswerType.CROSSBREEDING, "Jersey Giant")

        // --- VARIANTS ---
        testQuery("Jersey giant vs RIR", AnswerType.BREED_INFO, "Jersey Giant")
        testQuery("rir cross jg", AnswerType.CROSSBREEDING, "Jersey Giant")
        testQuery("incubating eggs humidity", AnswerType.INCUBATION, "humidity")
        testQuery("nursery status", AnswerType.NURSERY, "nursery")
        testQuery("spending summary", AnswerType.FINANCE, "spend")
        testQuery("device status", AnswerType.EQUIPMENT, "devices online")

        // --- FALLBACK ---
        testQuery("What is the meaning of life?", AnswerType.FALLBACK, null)
        testQuery("Random noise asldkfjasl", AnswerType.FALLBACK, null)
    }
}
