package com.example.hatchtracker.feature.community.devtools.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.repository.*
import com.example.hatchtracker.data.sync.MarketplaceFinanceSyncAdapter
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.domain.model.*
import com.example.hatchtracker.domain.repository.*
import com.example.hatchtracker.domain.service.EntityPassportSnapshotService
import com.example.hatchtracker.domain.service.ExpertiseSignalService
import com.example.hatchtracker.domain.service.InsightGeneratorService
import com.example.hatchtracker.domain.service.QuestionRoutingService
import com.example.hatchtracker.domain.service.ShareCardService
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class CommunityDevToolsViewModel @Inject constructor(
    private val subscriptionStateManager: SubscriptionStateManager,
    private val configRepository: ConfigRepository,
    private val userRepository: UserRepository,
    private val communityPostRepository: Lazy<CommunityPostRepository>,
    private val marketplaceRepository: Lazy<MarketplaceListingRepository>,
    private val complianceEvaluator: Lazy<MarketplaceComplianceEvaluator>,
    private val financeSyncAdapter: Lazy<MarketplaceFinanceSyncAdapter>,
    private val snapshotService: EntityPassportSnapshotService,
    private val expertiseSignalService: ExpertiseSignalService,
    private val shareCardService: ShareCardService,
    private val routingService: QuestionRoutingService,
    private val projectRepository: Lazy<CollaborativeBreedingProjectRepository>,
    private val insightGeneratorService: InsightGeneratorService,
    private val insightRepository: InsightRepository,
    private val incubationRepository: IncubationRepository,
    private val birdRepository: BirdRepository,
    private val flockRepository: FlockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityDevToolsUiState())
    val uiState: StateFlow<CommunityDevToolsUiState> = _uiState.asStateFlow()

    val canAccess: StateFlow<Boolean> = combine(
        subscriptionStateManager.isDeveloper,
        subscriptionStateManager.isAdmin,
        configRepository.observeAppAccessConfig()
    ) { isDev, isAdmin, config ->
        (isDev || isAdmin) && config.communityConfig.communityEnabled
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Section 1: Community Posts Testing ---
    fun createTestPost(body: String, kind: PostKind, visibility: Visibility) {
        viewModelScope.launch {
            val profile = userRepository.userProfile.firstOrNull() ?: return@launch
            val authorSnapshot = ProfileProjectionFactory.createAuthorSnapshot(profile)
            val post = CommunityPost(
                authorUserId = profile.userId,
                authorSnapshot = authorSnapshot,
                kind = kind,
                bodyText = body,
                visibility = visibility
            )
            communityPostRepository.get().createPost(post).onSuccess {
                _uiState.update { it.copy(lastOperationMessage = "Post Created Successfully") }
            }
        }
    }

    // --- Section 2: Comments Testing ---
    fun addTestComment(postId: String, text: String) {
        viewModelScope.launch {
            val profile = userRepository.userProfile.firstOrNull() ?: return@launch
            val authorSnapshot = ProfileProjectionFactory.createAuthorSnapshot(profile)
            val comment = CommunityComment(
                postId = postId,
                authorUserId = profile.userId,
                authorSnapshot = authorSnapshot,
                bodyText = text
            )
            communityPostRepository.get().addComment(postId, comment).onSuccess {
                _uiState.update { it.copy(lastOperationMessage = "Comment Added") }
            }
        }
    }

    // --- Section 4: Marketplace Listing Testing ---
    fun createTestListing(title: String, price: Double, category: ListingCategory) {
        viewModelScope.launch {
            val profile = userRepository.userProfile.firstOrNull() ?: return@launch
            val sellerSnapshot = ProfileProjectionFactory.createSellerSnapshot(profile)
            val listing = MarketplaceListing(
                sellerUserId = profile.userId,
                sellerSnapshot = sellerSnapshot,
                category = category,
                title = title,
                description = "Test listing description",
                price = price
            )
            marketplaceRepository.get().createListing(listing).onSuccess {
                _uiState.update { it.copy(lastOperationMessage = "Listing Created: $it") }
            }
        }
    }

    // --- Section 5: Sale Simulation ---
    fun simulateSale(listingId: String, amount: Double) {
        viewModelScope.launch {
            val profile = userRepository.userProfile.firstOrNull() ?: return@launch
            val sale = MarketplaceSale(
                id = "SIM-SALE-${System.currentTimeMillis()}",
                listingId = listingId,
                sellerUserId = profile.userId,
                buyerUserId = "test-buyer-id",
                amount = amount
            )
            financeSyncAdapter.get().syncSaleToFinance(sale).onSuccess {
                _uiState.update { it.copy(lastOperationMessage = "Sale Simulated & Synced") }
            }
        }
    }

    // --- Section 6: Profile Projection ---
    fun generateAuthorSnapshot(userId: String) {
        viewModelScope.launch {
            // In dev tools, we fetch the profile once
            val profile = userRepository.userProfile.firstOrNull() // Simplified for dev tool
            if (profile != null) {
                val snapshot = ProfileProjectionFactory.createAuthorSnapshot(profile)
                _uiState.update { it.copy(authorSnapshotPreview = snapshot) }
            }
        }
    }

    // --- Section 7: Entity Passport Snapshot ---
    fun generateEntitySnapshot(entityType: String, entityId: String, context: ShareContext) {
        // Implementation would fetch entity first, then call service
        // For dev toolkit, we can simulate or provide a mock bird for UI testing
        _uiState.update { it.copy(lastOperationMessage = "Entity Snapshot Gen triggered (Mocked in UI)") }
    }

    // --- Section 9: Compliance Testing ---
    fun testCompliance(category: ListingCategory) {
        viewModelScope.launch {
            val profile = userRepository.userProfile.firstOrNull() ?: return@launch
            val mockListing = MarketplaceListing(
                sellerUserId = profile.userId,
                sellerSnapshot = ProfileProjectionFactory.createSellerSnapshot(profile),
                category = category,
                title = "Compliance Test",
                description = "Test",
                price = 10.0
            )
            val status = complianceEvaluator.get().evaluate(mockListing)
            _uiState.update { it.copy(complianceResult = status.name) }
        }
    }

    // --- Section 10: Expertise & Projects Testing ---
    fun calculateExpertise() {
        viewModelScope.launch {
            // For dev tools, we simulate with empty lists to show logic works
            val signals = expertiseSignalService.calculateSignals(emptyList(), emptyList(), emptyList(), emptyList())
            val score = expertiseSignalService.calculateScore(signals)
            val level = expertiseSignalService.determineLevel(signals)
            
            _uiState.update { it.copy(
                expertiseResult = "Score: $score | Level: ${level.name}\nSignals: $signals",
                reputationSignals = signals
            ) }
        }
    }

    fun simulateShareCard(type: CardType) {
        // Mock generation for dev tool visualization
        val card = when(type) {
            CardType.HATCH_RESULT -> shareCardService.generateIncubationCard(Incubation(eggsCount = 10, hatchedCount = 8))
            else -> null
        }
        _uiState.update { it.copy(shareCardPreview = card) }
    }

    fun testRouting(question: String) {
        val topics = routingService.detectTopics(question)
        _uiState.update { it.copy(routingResult = "Detected Topics: ${topics.joinToString()}") }
    }

    // --- Section 13: Daily Insights Testing ---
    fun triggerDailyInsights(dateKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, lastOperationMessage = "Generating insights...") }
            
            val incubations = incubationRepository.getAllIncubations()
            val birds = birdRepository.getAllBirds().first()
            val flocks = flockRepository.allActiveFlocks.first()
            val posts = communityPostRepository.get().getFeed(limit = 100).first()

            val result = insightGeneratorService.generateDailyBatch(
                dateKey = dateKey,
                incubations = incubations,
                birds = birds,
                flocks = flocks,
                communityQuestions = posts
            )

            _uiState.update { 
                it.copy(
                    isLoading = false,
                    lastOperationMessage = "Generation Complete: ${result.insights.size} insights created.",
                    insightGenerationResult = result
                ) 
            }
        }
    }

    fun previewLowConfidenceInsight() {
        val now = System.currentTimeMillis()
        _uiState.update { 
            it.copy(
                insightGenerationResult = com.example.hatchtracker.domain.service.InsightGenerationResult(
                    batch = com.example.hatchtracker.domain.model.DailyInsightBatch(
                        dateKey = "2026-03-06",
                        totalInsightCount = 1,
                        generatedAt = now,
                        sourceWindowStart = now - 86_400_000L,
                        sourceWindowEnd = now
                    ),
                    insights = listOf(
                        com.example.hatchtracker.domain.model.DailyInsightItem(
                            id = "low-conf-preview",
                            title = "Possible Breed Trend detected",
                            body = "Hatchy noticed a small increase in Brahma breeding activity. This might be an emerging trend.",
                            type = com.example.hatchtracker.domain.model.InsightType.BREED_TREND,
                            confidenceLevel = com.example.hatchtracker.domain.model.ConfidenceLevel.LOW,
                            rankingScore = 0.5,
                            ruleId = "low-conf-test",
                            windowSummary = com.example.hatchtracker.domain.model.InsightWindowSummary(
                                sourceWindowStart = now - 86_400_000L,
                                sourceWindowEnd = now
                            )
                        )
                    ),
                    candidates = emptyList(),
                    suppressed = emptyList()
                )
            )
        }
    }
    fun toggleReducedMotion() {
        _uiState.update { it.copy(isReducedMotionMode = !it.isReducedMotionMode) }
    }

    fun triggerPulseSimulation() {
        _uiState.update { it.copy(pulseKey = "pulse-${System.currentTimeMillis()}") }
    }

    fun triggerNudgeSimulation() {
        _uiState.update { it.copy(nudgeKey = "nudge-${System.currentTimeMillis()}") }
    }
}

data class CommunityDevToolsUiState(
    val lastOperationMessage: String = "",
    val authorSnapshotPreview: CommunityAuthorSnapshot? = null,
    val complianceResult: String = "",
    val expertiseResult: String = "",
    val reputationSignals: ReputationSignals? = null,
    val shareCardPreview: ShareableCard? = null,
    val routingResult: String = "",
    val isLoading: Boolean = false,
    val insightGenerationResult: com.example.hatchtracker.domain.service.InsightGenerationResult? = null,
    val isReducedMotionMode: Boolean = false,
    val pulseKey: String = "",
    val nudgeKey: String = ""
)
