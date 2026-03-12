package com.example.hatchtracker.feature.community.devtools.ui

import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.models.AppAccessConfig
import com.example.hatchtracker.data.models.CommunityConfig
import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.repository.CollaborativeBreedingProjectRepository
import com.example.hatchtracker.data.repository.ConfigRepository
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.data.repository.InsightRepository
import com.example.hatchtracker.data.repository.UserRepository
import com.example.hatchtracker.data.sync.MarketplaceFinanceSyncAdapter
import com.example.hatchtracker.domain.repository.CommunityPostRepository
import com.example.hatchtracker.domain.repository.MarketplaceComplianceEvaluator
import com.example.hatchtracker.domain.repository.MarketplaceListingRepository
import com.example.hatchtracker.domain.service.EntityPassportSnapshotService
import com.example.hatchtracker.domain.service.ExpertiseSignalService
import com.example.hatchtracker.domain.service.InsightGeneratorService
import com.example.hatchtracker.domain.service.QuestionRoutingService
import com.example.hatchtracker.domain.service.ShareCardService
import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityDevToolsViewModelTest {

    private lateinit var subscriptionStateManager: SubscriptionStateManager
    private lateinit var configRepository: ConfigRepository

    private val isDevFlow = MutableStateFlow(false)
    private val isAdminFlow = MutableStateFlow(false)
    private val configFlow = MutableStateFlow(
        AppAccessConfig(communityConfig = CommunityConfig(communityEnabled = false))
    )
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: CommunityDevToolsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        subscriptionStateManager = mockk(relaxed = true)
        configRepository = mockk(relaxed = true)

        every { subscriptionStateManager.isDeveloper } returns isDevFlow
        every { subscriptionStateManager.isAdmin } returns isAdminFlow
        every { configRepository.observeAppAccessConfig() } returns configFlow

        viewModel = CommunityDevToolsViewModel(
            subscriptionStateManager = subscriptionStateManager,
            configRepository = configRepository,
            userRepository = mockk(relaxed = true),
            communityPostRepository = Lazy { mockk<CommunityPostRepository>(relaxed = true) },
            marketplaceRepository = Lazy { mockk<MarketplaceListingRepository>(relaxed = true) },
            complianceEvaluator = Lazy { mockk<MarketplaceComplianceEvaluator>(relaxed = true) },
            financeSyncAdapter = Lazy { mockk<MarketplaceFinanceSyncAdapter>(relaxed = true) },
            snapshotService = mockk<EntityPassportSnapshotService>(relaxed = true),
            expertiseSignalService = mockk<ExpertiseSignalService>(relaxed = true),
            shareCardService = mockk<ShareCardService>(relaxed = true),
            routingService = mockk<QuestionRoutingService>(relaxed = true),
            projectRepository = Lazy { mockk<CollaborativeBreedingProjectRepository>(relaxed = true) },
            insightGeneratorService = mockk<InsightGeneratorService>(relaxed = true),
            insightRepository = mockk<InsightRepository>(relaxed = true),
            incubationRepository = mockk<IncubationRepository>(relaxed = true),
            birdRepository = mockk<BirdRepository>(relaxed = true),
            flockRepository = mockk<FlockRepository>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `canAccess is false when community is disabled`() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.canAccess.collect {}
        }
        isDevFlow.value = true
        configFlow.value = AppAccessConfig(communityConfig = CommunityConfig(communityEnabled = false))
        advanceUntilIdle()

        assertFalse(viewModel.canAccess.value)
        collectJob.cancel()
    }

    @Test
    fun `canAccess is false when user has no dev-admin roles`() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.canAccess.collect {}
        }
        isDevFlow.value = false
        isAdminFlow.value = false
        configFlow.value = AppAccessConfig(communityConfig = CommunityConfig(communityEnabled = true))
        advanceUntilIdle()

        assertFalse(viewModel.canAccess.value)
        collectJob.cancel()
    }

    @Test
    fun `canAccess is true for Developer when community enabled`() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.canAccess.collect {}
        }
        isDevFlow.value = true
        configFlow.value = AppAccessConfig(communityConfig = CommunityConfig(communityEnabled = true))
        advanceUntilIdle()

        assertTrue(viewModel.canAccess.value)
        collectJob.cancel()
    }

    @Test
    fun `canAccess is true for Admin when community enabled`() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.canAccess.collect {}
        }
        isAdminFlow.value = true
        configFlow.value = AppAccessConfig(communityConfig = CommunityConfig(communityEnabled = true))
        advanceUntilIdle()

        assertTrue(viewModel.canAccess.value)
        collectJob.cancel()
    }
}
