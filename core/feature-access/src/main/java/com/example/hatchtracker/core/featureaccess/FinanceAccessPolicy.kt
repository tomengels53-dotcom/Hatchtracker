package com.example.hatchtracker.core.featureaccess

import com.example.hatchtracker.data.models.SubscriptionTier

/**
 * Single source of truth for financial feature gating across the application.
 */
object FinanceAccessPolicy {

    /**
     * Defines whether a user can see advanced financial insights:
     * filters, categories, charts, and detailed ledger lists.
     * Accessible only to PRO, EXPERT, and Admin/Dev statuses.
     */
    fun canViewFinanceInsights(
        tier: SubscriptionTier,
        isAdmin: Boolean
    ): Boolean {
        if (isAdmin) return true

        return tier == SubscriptionTier.PRO || tier == SubscriptionTier.EXPERT
    }

    /**
     * Defines whether a user can add or delete basic income/expense entries.
     * Accessible to all registered users (FREE, PRO, EXPERT).
     *
     * Note: "FREE" users are allowed to see total sums and a basic
     * chronological ledger, and can manage their own entries.
     */
    @Suppress("UNUSED_PARAMETER")
    fun canEditFinanceEntries(
        tier: SubscriptionTier,
        isAdmin: Boolean
    ): Boolean {
        // As long as they have an account, they can manage basic finance entries
        return true
    }
}
