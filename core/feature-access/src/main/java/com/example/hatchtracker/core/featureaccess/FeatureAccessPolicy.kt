package com.example.hatchtracker.core.featureaccess

import com.example.hatchtracker.data.models.SubscriptionTier

enum class FeatureKey {
    FLOCKS,
    INCUBATION,
    NURSERY,
    BREEDING,
    FINANCE,
    SUPPORT,
    DEVICES,
    ADMIN,
    COMMUNITY,
    FINANCIAL_INSIGHTS,
    // Breeding UX Overhaul
    BREEDING_SCENARIOS_CREATE,
    BREEDING_SCENARIOS_SIMULATE,
    BREEDING_ACTION_PLANS
}

data class AccessDecision(
    val allowed: Boolean,
    val reason: String?,
    val requiredTier: SubscriptionTier?
)

object FeatureAccessPolicy {
    fun canAccess(
        feature: FeatureKey,
        tier: SubscriptionTier,
        isAdmin: Boolean
    ): AccessDecision {
        if (isAdmin) {
            return AccessDecision(true, null, null)
        }

        return when (feature) {
            FeatureKey.BREEDING_SCENARIOS_CREATE,
            FeatureKey.BREEDING_SCENARIOS_SIMULATE,
            FeatureKey.BREEDING_ACTION_PLANS -> {
                if (tier == SubscriptionTier.PRO) {
                    AccessDecision(true, null, null)
                } else {
                    AccessDecision(false, "PRO subscription required for advanced breeding tools", SubscriptionTier.PRO)
                }
            }
            FeatureKey.BREEDING -> AccessDecision(true, null, null)
            FeatureKey.FINANCE -> {
                if (tier == SubscriptionTier.EXPERT || tier == SubscriptionTier.PRO) {
                    AccessDecision(true, null, null)
                } else {
                    AccessDecision(false, "EXPERT subscription required", SubscriptionTier.EXPERT)
                }
            }
            FeatureKey.FINANCIAL_INSIGHTS -> {
                if (tier == SubscriptionTier.EXPERT || tier == SubscriptionTier.PRO) {
                    AccessDecision(true, null, null)
                } else {
                    AccessDecision(false, "EXPERT subscription required", SubscriptionTier.EXPERT)
                }
            }
            FeatureKey.ADMIN -> {
                if (isAdmin) {
                    AccessDecision(true, null, null)
                } else {
                    AccessDecision(false, "System admin required", null)
                }
            }
            else -> AccessDecision(true, null, null)
        }
    }
}
