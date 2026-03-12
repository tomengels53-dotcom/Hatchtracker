package com.example.hatchtracker.billing

import com.example.hatchtracker.auth.SessionManager
import com.example.hatchtracker.auth.SessionState
import com.example.hatchtracker.data.models.AppAccessConfig
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.domain.model.UserProfile
import com.example.hatchtracker.data.repository.AuthClaimsRepository
import com.example.hatchtracker.data.repository.BillingRepository
import com.example.hatchtracker.data.repository.ConfigRepository
import com.example.hatchtracker.data.repository.UserRepository
import com.example.hatchtracker.domain.entitlement.EntitlementResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionStateManagerTest {

    private val billingRepository: BillingRepository = mockk(relaxed = true)
    private val configRepository: ConfigRepository = mockk(relaxed = true)
    private val authClaimsRepository: AuthClaimsRepository = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val entitlementResolver = EntitlementResolver()

    private val testDispatcher = StandardTestDispatcher()

    private val claimsIsAdminFlow = MutableStateFlow(false)
    private val claimsIsDeveloperFlow = MutableStateFlow(false)
    private val userProfileFlow = MutableStateFlow<UserProfile?>(null)
    
    private val billingActiveFlow = MutableStateFlow(false)
    private val billingTierFlow = MutableStateFlow(SubscriptionTier.FREE)
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { authClaimsRepository.isAdmin } returns claimsIsAdminFlow
        every { authClaimsRepository.isDeveloper } returns claimsIsDeveloperFlow
        every { userRepository.userProfile } returns userProfileFlow
        
        every { billingRepository.activeSubscription } returns billingActiveFlow
        every { billingRepository.subscriptionTier } returns billingTierFlow
        every { configRepository.observeAppAccessConfig() } returns MutableStateFlow(AppAccessConfig())
        every { sessionManager.sessionState } returns MutableStateFlow(SessionState.Unauthenticated)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun awaitCondition(
        timeoutMs: Long = 1_000,
        predicate: () -> Boolean
    ) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(timeoutMs) {
                while (!predicate()) {
                    delay(10)
                }
            }
        }
    }

    @Test
    fun `Admin flag correctly resolves combining claims and profile`() = runTest {
        val manager = SubscriptionStateManager(
            billingRepository, configRepository, authClaimsRepository, 
            sessionManager, userRepository, entitlementResolver
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.isAdmin.collect {}
        }

        // Initial state
        assertFalse("Admin invariant must start false", manager.isAdmin.value)

        // 1. Claims admin becomes true
        claimsIsAdminFlow.value = true
        awaitCondition { manager.isAdmin.value }
        assertTrue("Admin invariant true from claims", manager.isAdmin.value)

        // 2. Profile admin is true but claims is false
        claimsIsAdminFlow.value = false
        userProfileFlow.value = UserProfile(userId = "uid", isSystemAdmin = true)
        awaitCondition { manager.isAdmin.value }
        assertTrue("Admin invariant true from profile", manager.isAdmin.value)

        collectJob.cancel()
    }

    @Test
    fun `Ads visibility gating invariant resolves correctly based on effective tier and roles`() = runTest {
        val manager = SubscriptionStateManager(
            billingRepository, configRepository, authClaimsRepository, 
            sessionManager, userRepository, entitlementResolver
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.shouldShowAds.collect {}
        }

        val adminCollectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.isProOrAbove() // Just mapping flow under the hood? Actually it checks value. Let's collect effectiveTier
            manager.effectiveTier.collect {}
        }

        // Free tier -> Ads enabled
        billingActiveFlow.value = false
        billingTierFlow.value = SubscriptionTier.FREE
        claimsIsAdminFlow.value = false
        claimsIsDeveloperFlow.value = false
        awaitCondition { manager.shouldShowAds.value }
        
        assertTrue("Ads invariant: Free tier sees ads", manager.shouldShowAds.value)
        assertFalse("Pro gating invariant: Free is not Pro", manager.isProOrAbove())

        // Expert tier -> Ads disabled
        billingActiveFlow.value = true
        billingTierFlow.value = SubscriptionTier.EXPERT
        awaitCondition {
            !manager.shouldShowAds.value && manager.isProOrAbove() && manager.isExpert()
        }
        
        assertFalse("Ads invariant: Premium tiers do not see ads", manager.shouldShowAds.value)
        assertTrue("Pro gating invariant: Expert is >= Pro", manager.isProOrAbove())
        assertTrue("Expert gating invariant", manager.isExpert())

        // Free tier but user is Admin -> Ads disabled
        billingActiveFlow.value = false
        billingTierFlow.value = SubscriptionTier.FREE
        claimsIsAdminFlow.value = true
        awaitCondition { !manager.shouldShowAds.value }
        
        assertFalse("Ads invariant: Admins do not see ads", manager.shouldShowAds.value)

        collectJob.cancel()
        adminCollectJob.cancel()
    }
}
