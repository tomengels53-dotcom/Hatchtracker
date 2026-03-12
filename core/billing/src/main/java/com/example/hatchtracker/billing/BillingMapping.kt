package com.example.hatchtracker.billing

import com.example.hatchtracker.data.models.SubscriptionTier

/**
 * Central source of truth for mapping Google Play Product IDs to Application Subscription Tiers.
 * This mapping is immutable at runtime.
 */
object BillingMapping {

    // Google Play Product IDs
    const val PRODUCT_EXPERT_MONTHLY = "subscription_expert_monthly"
    const val PRODUCT_EXPERT_YEARLY = "subscription_expert_yearly"
    const val PRODUCT_PRO_MONTHLY = "subscription_pro_monthly"
    const val PRODUCT_PRO_YEARLY = "subscription_pro_yearly"

    // Firestore / App Internal Tier IDs
    const val TIER_FREE = "free"
    const val TIER_EXPERT = "expert"
    const val TIER_PRO = "pro"

    /**
     * Maps a Google Play Product ID to an internal SubscriptionTier.
     */
    private val productToTierMap: Map<String, SubscriptionTier> = mapOf(
        PRODUCT_EXPERT_MONTHLY to SubscriptionTier.EXPERT,
        PRODUCT_EXPERT_YEARLY to SubscriptionTier.EXPERT,
        PRODUCT_PRO_MONTHLY to SubscriptionTier.PRO,
        PRODUCT_PRO_YEARLY to SubscriptionTier.PRO
    )

    /**
     * Gets the corresponding SubscriptionTier for a Product ID.
     * Returns FREE if no mapping is found.
     */
    fun getTierForProduct(productId: String): SubscriptionTier {
        return productToTierMap[productId] ?: SubscriptionTier.FREE
    }

    /**
     * Gets the Firestore identifier for a SubscriptionTier.
     */
    fun getFirestoreId(tier: SubscriptionTier): String = when (tier) {
        SubscriptionTier.FREE -> TIER_FREE
        SubscriptionTier.EXPERT -> TIER_EXPERT
        SubscriptionTier.PRO -> TIER_PRO
    }
}
