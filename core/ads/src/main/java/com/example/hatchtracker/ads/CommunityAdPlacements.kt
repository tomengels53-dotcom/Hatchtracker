package com.example.hatchtracker.ads

/**
 * Dormant ad placements for future Community and Marketplace features.
 * These are not yet active in any UI and do not initialize the ad SDK.
 */
enum class CommunityAdPlacement(val placementId: String) {
    /**
     * Inline ad shown within the social feed.
     */
    FEED_INLINE("ca-app-pub-placeholder/feed-inline"),
    
    /**
     * Banner shown at the top or bottom of marketplace listings.
     */
    MARKETPLACE_BANNER("ca-app-pub-placeholder/marketplace-banner"),
    
    /**
     * Interstitial or native ad shown between community posts.
     */
    SOCIAL_INTERSTITIAL("ca-app-pub-placeholder/social-interstitial")
}
