package com.example.hatchtracker.data.models

enum class FetchStatus {
    SUCCESS, NOT_FOUND, PERMISSION_DENIED, ERROR, INITIALIZING
}

/**
 * Configuration for community and marketplace features.
 */
data class CommunityConfig(
    val communityEnabled: Boolean = false,
    val feedEnabled: Boolean = false,
    val postingEnabled: Boolean = false,
    val commentsEnabled: Boolean = false,
    val reactionsEnabled: Boolean = false,
    val reportingEnabled: Boolean = false,
    val marketplaceEnabled: Boolean = false,
    val marketplaceEquipmentEnabled: Boolean = false,
    val marketplaceEggSalesEnabled: Boolean = false,
    val marketplaceLiveAnimalSalesEnabled: Boolean = false,
    val videoUploadEnabled: Boolean = false,
    val directMessagesEnabled: Boolean = false,
    val communityProjectsEnabled: Boolean = false,
    val communityExpertiseEnabled: Boolean = false,
    val communityMarketplaceTrustEnabled: Boolean = false,
    val communityQuestionRoutingEnabled: Boolean = false,
    val communityShareCardsEnabled: Boolean = false,
    val moderationEnabled: Boolean = false,
    val moderationQueueEnabled: Boolean = false,
    val marketplaceModerationEnabled: Boolean = false,
    val userBlockingEnabled: Boolean = false,
    val strikeSystemEnabled: Boolean = false,
    val insightsEnabled: Boolean = false
)

/**
 * Configuration for reviewer access overlay.
 */
data class AppAccessConfig(
    val reviewerOverlayEnabled: Boolean = false,
    val reviewerAllowlistUids: List<String> = emptyList(),
    val reviewerTier: SubscriptionTier = SubscriptionTier.FREE,
    val expiresAt: Long? = null,
    val lastUpdated: Long? = null,
    val fetchStatus: FetchStatus = FetchStatus.INITIALIZING,
    val fetchTimestamp: Long = 0,
    val errorMessage: String? = null,
    val communityConfig: CommunityConfig = CommunityConfig()
) {
    fun isOverlayActive(now: Long = System.currentTimeMillis()): Boolean {
        return reviewerOverlayEnabled && (expiresAt == null || now <= expiresAt)
    }
}
