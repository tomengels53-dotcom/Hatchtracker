package com.example.hatchtracker.billing

import com.example.hatchtracker.auth.SessionManager
import com.example.hatchtracker.auth.SessionState
import com.example.hatchtracker.data.models.AppAccessConfig
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.data.repository.AuthClaimsRepository
import com.example.hatchtracker.data.repository.BillingRepository
import com.example.hatchtracker.data.repository.ConfigRepository
import com.example.hatchtracker.data.repository.UserRepository
import com.example.hatchtracker.domain.entitlement.EntitlementResolver
import com.example.hatchtracker.domain.entitlement.EntitlementResolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized manager for reactive subscription state.
 * Coordinates billing, reviewer overlays, and auth state to produce the effective tier.
 */
@Singleton
class SubscriptionStateManager @Inject constructor(
    private val billingRepository: BillingRepository,
    private val configRepository: ConfigRepository,
    private val authClaimsRepository: AuthClaimsRepository,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val entitlementResolver: EntitlementResolver
) {
    private data class EntitlementInput(
        val billingActive: Boolean,
        val billingTier: SubscriptionTier,
        val config: AppAccessConfig,
        val uid: String?,
        val isAdmin: Boolean
    )

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Exposes reactive admin status using claims as primary and profile flags as local fallback.
     */
    val isAdmin: StateFlow<Boolean> = combine(
        authClaimsRepository.isAdmin,
        userRepository.userProfile.map { profile ->
            profile?.isSystemAdmin == true ||
                profile?.roles?.any { it.equals("admin", ignoreCase = true) || it.equals("system_admin", ignoreCase = true) } == true
        }
    ) { fromClaims, fromProfile ->
        fromClaims || fromProfile
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * Exposes reactive developer status using claims as primary and profile flags as local fallback.
     */
    val isDeveloper: StateFlow<Boolean> = combine(
        authClaimsRepository.isDeveloper,
        userRepository.userProfile.map { profile ->
            profile?.isDeveloper == true ||
                profile?.roles?.any { it.equals("developer", ignoreCase = true) } == true
        }
    ) { fromClaims, fromProfile ->
        fromClaims || fromProfile
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * Exposes reactive community admin status using claims as primary and profile flags as local fallback.
     */
    val isCommunityAdmin: StateFlow<Boolean> = combine(
        authClaimsRepository.isCommunityAdmin,
        userRepository.userProfile.map { profile ->
            profile?.isCommunityAdmin == true ||
                profile?.roles?.any { it.equals("community_admin", ignoreCase = true) } == true
        }
    ) { fromClaims, fromProfile ->
        fromClaims || fromProfile
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * The single source of truth for the user's effective subscription tier.
     * Priority: Billing > Secure Overlay > Free.
     */
    val entitlementResolution: StateFlow<EntitlementResolution> = combine(
        billingRepository.activeSubscription,
        billingRepository.subscriptionTier,
        configRepository.observeAppAccessConfig(),
        sessionManager.sessionState.map { (it as? SessionState.Authenticated)?.user?.uid },
        isAdmin
    ) { billingActive, billingTier, config, uid, admin ->
        EntitlementInput(
            billingActive = billingActive,
            billingTier = billingTier,
            config = config,
            uid = uid,
            isAdmin = admin
        )
    }.combine(isDeveloper) { input, dev ->
        entitlementResolver.resolve(
            billingActive = input.billingActive,
            realSubscriptionTier = input.billingTier,
            appAccessConfig = input.config,
            currentUid = input.uid,
            isAdmin = input.isAdmin,
            isDeveloper = dev,
            nowMillis = System.currentTimeMillis()
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EntitlementResolution(
            tier = SubscriptionTier.FREE,
            reason = "Initializing..."
        )
    )

    /**
     * Effective tier derived from the full resolution.
     */
    val effectiveTier: StateFlow<SubscriptionTier> = entitlementResolution
        .map { it.tier }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubscriptionTier.FREE
        )

    /**
     * Derived flow for capabilities based on the effective tier.
     */
    val currentCapabilities: StateFlow<SubscriptionCapabilities> = effectiveTier
        .map { SubscriptionCapabilities.getForTier(it) }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubscriptionCapabilities.getForTier(SubscriptionTier.FREE)
        )

    val activePlayProductId: StateFlow<String?> = billingRepository.activeSubscriptionProductId
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val lastPlaySyncEpochMs: StateFlow<Long> = billingRepository.lastPlaySyncEpochMs
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    /**
     * Helper flow to determine if ads should be displayed.
     * Ads are disabled for higher tiers OR for staff (admins/developers).
     */
    val shouldShowAds: StateFlow<Boolean> = combine(
        currentCapabilities,
        isAdmin,
        isDeveloper
    ) { caps, admin, dev ->
        caps.isAdsEnabled && !admin && !dev
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    // Helper methods for UI convenience
    fun isProOrAbove(): Boolean = when (effectiveTier.value) {
        SubscriptionTier.FREE -> false
        SubscriptionTier.EXPERT, SubscriptionTier.PRO -> true
    }
    fun isExpert(): Boolean = effectiveTier.value == SubscriptionTier.EXPERT
}
