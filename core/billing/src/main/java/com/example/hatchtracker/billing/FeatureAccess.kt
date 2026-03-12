package com.example.hatchtracker.billing

import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.domain.model.UserProfile
import com.example.hatchtracker.core.billing.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central authority for enforcing feature access limits based on subscription tier.
 * This class ensures that all feature gates are consistent and easily maintainable.
 */
@Singleton
class FeatureAccess @Inject constructor(
    private val subscriptionStateManager: SubscriptionStateManager
) {

    private val currentCapabilities: SubscriptionCapabilities
        get() = subscriptionStateManager.currentCapabilities.value

    /**
     * Determines if the user can create another incubation record.
     */
    fun canCreateMoreThanNIncubations(currentCount: Int): Boolean {
        return currentCount < currentCapabilities.maxActiveIncubations
    }

    /**
     * Determines if the user can access selective breeding tools.
     */
    fun canUseSelectiveBreeding(): Boolean {
        return currentCapabilities.isSelectiveBreedingEnabled
    }

    /**
     * Determines if the user can access parent -> offspring linking features.
     */
    fun canUseParentLinking(): Boolean {
        return currentCapabilities.isParentOffspringLinkingEnabled
    }

    /**
     * Determines if the user can access the full breed standards and physical metadata.
     */
    fun canAccessBreedStandards(): Boolean {
        // Assuming breed standards are tied to selective breeding/pro features
        return currentCapabilities.tier.ordinal >= SubscriptionTier.EXPERT.ordinal
    }

    /**
     * Determines if the user can export their data to external formats.
     */
    fun canExportData(): Boolean {
        return currentCapabilities.canExportData
    }

    /**
     * Determines if the user can access the Breeding module (PRO-only).
     */
    fun canAccessBreeding(profile: UserProfile? = null): Boolean {
        val tier = effectiveTier(profile)
        val privilege = subscriptionStateManager.isAdmin.value || subscriptionStateManager.isDeveloper.value || BuildConfig.DEBUG
        return FeatureAccessPolicy.canAccess(FeatureKey.BREEDING, tier, privilege).allowed
    }

    /**
     * Determines if the user can access Finance (EXPERT+PRO).
     */
    fun canAccessFinance(profile: UserProfile? = null): Boolean {
        val tier = effectiveTier(profile)
        val privilege = subscriptionStateManager.isAdmin.value || subscriptionStateManager.isDeveloper.value || BuildConfig.DEBUG
        return FeatureAccessPolicy.canAccess(FeatureKey.FINANCE, tier, privilege).allowed
    }

    private fun effectiveTier(profile: UserProfile?): SubscriptionTier {
        if (profile != null && profile.subscriptionActive) {
            return profile.subscriptionTier
        }
        return subscriptionStateManager.effectiveTier.value
    }
}
