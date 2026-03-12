package com.example.hatchtracker.ads

import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for ad visibility and initialization.
 * Reacts to Firestore changes in real-time.
 */
@Singleton
class AdsManager @Inject constructor(
    private val subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager
) {
    companion object {
        private const val TAG = "AdsManager"
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // Test ID
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _shouldShowAds = MutableStateFlow(false)
    val shouldShowAds: StateFlow<Boolean> = _shouldShowAds.asStateFlow()

    private var isInitialized = false

    /**
     * Initializes the AdsManager.
     * Starts listening to subscription state immediately.
     */
    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        Logger.d(TAG, "Initializing AdsManager...")

        // Observe SubscriptionStateManager for unified ad visibility logic
        scope.launch {
            subscriptionStateManager.shouldShowAds
                .collectLatest { adsEnabled ->
                    Logger.d(TAG, "Effective Ads Enabled Status: $adsEnabled")
                    _shouldShowAds.value = adsEnabled
                }
        }
    }
}
