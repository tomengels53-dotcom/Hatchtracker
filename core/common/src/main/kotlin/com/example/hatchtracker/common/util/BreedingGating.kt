package com.example.hatchtracker.common.util

import com.example.hatchtracker.domain.model.UserProfile
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.data.models.SubscriptionTier

/**
 * Utility to enforce PRO-only access for the Breeding module.
 */
object BreedingGating {
    /**
     * Checks if a user has access to premium breeding features.
     * Developers and PRO subscribers always have access.
     */
    fun hasProAccess(profile: UserProfile?): Boolean {
        val tier = if (profile?.subscriptionActive == true) {
            profile.subscriptionTier.name.let { SubscriptionTier.valueOf(it) }
        } else {
            SubscriptionTier.FREE
        }
        val isAdmin = profile?.isSystemAdmin == true || profile?.isDeveloper == true
        return FeatureAccessPolicy.canAccess(FeatureKey.BREEDING, tier, isAdmin).allowed
    }
}
