package com.example.hatchtracker.billing

import com.example.hatchtracker.data.models.SubscriptionTier

/**
 * Metadata for a subscription plan, including pricing.
 * Prices are numerical and localized at render time (No FX conversion).
 */
data class SubscriptionPlan(
    val tier: SubscriptionTier,
    val basePrice: Double,
    val period: String, // e.g. "month", "year"
    val productId: String
) {
    companion object {
        val PLANS = listOf(
            SubscriptionPlan(SubscriptionTier.EXPERT, 4.99, "month", BillingMapping.PRODUCT_EXPERT_MONTHLY),
            SubscriptionPlan(SubscriptionTier.PRO, 9.99, "month", BillingMapping.PRODUCT_PRO_YEARLY)
        )

        fun getForTier(tier: SubscriptionTier) = PLANS.find { it.tier == tier }
    }
}
