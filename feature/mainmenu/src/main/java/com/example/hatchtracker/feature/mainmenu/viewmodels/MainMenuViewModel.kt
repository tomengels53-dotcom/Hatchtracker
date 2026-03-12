package com.example.hatchtracker.feature.mainmenu.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.ads.AdManager
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.data.repository.NurseryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val subscriptionStateManager: SubscriptionStateManager,
    private val nurseryRepository: NurseryRepository,
    private val adManager: AdManager,
    private val breedStandardRepository: com.example.hatchtracker.data.repository.BreedStandardRepository,
    private val insightRepository: dagger.Lazy<com.example.hatchtracker.data.repository.InsightRepository>,
    private val configRepository: dagger.Lazy<com.example.hatchtracker.data.repository.ConfigRepository>,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    sealed class HomeContextState {
        object Loading : HomeContextState()
        data class Insight(val projection: com.example.hatchtracker.domain.model.InsightFeedProjection) : HomeContextState()
        object Lifecycle : HomeContextState()
    }

    private val _homeContextState = MutableStateFlow<HomeContextState>(HomeContextState.Loading)
    val homeContextState: StateFlow<HomeContextState> = _homeContextState.asStateFlow()

    private val _assistNudgeKey = MutableStateFlow<String?>(null)
    val assistNudgeKey: StateFlow<String?> = _assistNudgeKey.asStateFlow()

    private var cachedInsight: com.example.hatchtracker.domain.model.InsightFeedProjection? = null
    private var lastNudgedInsightId: String? = null

    init {
        loadHomeInsight()
    }

    private fun loadHomeInsight() {
        viewModelScope.launch {
            // 1. Check feature flag first
            val config = configRepository.get().observeAppAccessConfig().first()
            if (!config.communityConfig.insightsEnabled) {
                _homeContextState.value = HomeContextState.Lifecycle
                return@launch
            }

            // 2. Return cache if available (No nudge for cache)
            cachedInsight?.let {
                _homeContextState.value = HomeContextState.Insight(it)
                return@launch
            }

            // 3. Fetch with timeout fallback
            try {
                val insight = withTimeoutOrNull(400) {
                    insightRepository.get().getFeedProjections(limit = 1)
                        .map { list -> 
                            list.firstOrNull { 
                                it.feedSurface == "HOME_FEATURED"
                            }
                        }
                        .first()
                }

                if (insight != null) {
                    cachedInsight = insight
                    _homeContextState.value = HomeContextState.Insight(insight)
                    
                    // Trigger nudge only if not nudged before for this ID
                    if (lastNudgedInsightId != insight.id) {
                        lastNudgedInsightId = insight.id
                        _assistNudgeKey.value = insight.id
                    }
                } else {
                    _homeContextState.value = HomeContextState.Lifecycle
                }
            } catch (e: Exception) {
                _homeContextState.value = HomeContextState.Lifecycle
            }
        }
    }

    fun triggerAssistNudgeSimulation() {
        _assistNudgeKey.value = "sim-${System.currentTimeMillis()}"
    }

    fun getBreedsForSpecies(species: String): List<com.example.hatchtracker.data.models.BreedStandard> {
        return breedStandardRepository.getBreedsForSpecies(species)
    }

    fun addManualFlocklet(flocklet: com.example.hatchtracker.data.models.Flocklet) {
        viewModelScope.launch {
            val created = nurseryRepository.addFlocklet(flocklet)
            val caps = subscriptionStateManager.currentCapabilities.value
            val isAdmin = subscriptionStateManager.isAdmin.value
            val isDeveloper = subscriptionStateManager.isDeveloper.value
            val canScheduleNursery = FeatureAccessPolicy
                .canAccess(FeatureKey.NURSERY, caps.tier, isAdmin || isDeveloper)
                .allowed
            NotificationHelper.scheduleNurseryMilestones(
                context = appContext,
                flocklet = created,
                canSchedule = canScheduleNursery
            )
        }
    }

    // Subscription & Ads
    val shouldShowAds = subscriptionStateManager.shouldShowAds
    val currentCapabilities = subscriptionStateManager.currentCapabilities
    val effectiveTier = subscriptionStateManager.effectiveTier

    // Auth (Wrapping Singleton for UI consistency)
    val currentUser = UserAuthManager.currentUser
    val isAdmin = subscriptionStateManager.isAdmin
    val isDeveloper = subscriptionStateManager.isDeveloper
    val unauthorizedAdmins = UserAuthManager.unauthorizedAdmins
    
    // Combined Admin Access
    val canAccessAdmin = kotlinx.coroutines.flow.combine(isAdmin, isDeveloper) { admin, dev ->
        admin || dev
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), isAdmin.value || isDeveloper.value)

    // Nursery Data
    val activeFlocklets = nurseryRepository.activeFlocklets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ad Management
    fun initializeAds() {
        try {
            adManager.initialize()
        } catch (e: Exception) {
            // Log silently, don't crash UI
        }
    }

    // Auth Actions
    fun signOut() {
        UserAuthManager.signOut()
    }
}
