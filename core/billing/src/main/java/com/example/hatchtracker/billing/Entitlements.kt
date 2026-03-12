package com.example.hatchtracker.billing

import com.example.hatchtracker.data.models.SubscriptionTier
import kotlinx.coroutines.flow.StateFlow

/**
 * Default implementation of Entitlements using SubscriptionStateManager.
 */
class DefaultEntitlements(
    private val subscriptionStateManager: SubscriptionStateManager
) : Entitlements {
    
    override val tier: StateFlow<SubscriptionTier> = subscriptionStateManager.effectiveTier
    override val capabilities: StateFlow<SubscriptionCapabilities> = subscriptionStateManager.currentCapabilities
    
    override fun canAccessPROIntelligence(): Boolean {
        return capabilities.value.tier == SubscriptionTier.PRO
    }
    
    override fun maxBreeds(): Int {
        return capabilities.value.maxBreedsPerBatch
    }
    
    override fun canUseSelectiveBreeding(): Boolean {
        return capabilities.value.isSelectiveBreedingEnabled
    }
    
    override fun canLinkParentOffspring(): Boolean {
        return capabilities.value.isParentOffspringLinkingEnabled
    }
    
    override fun canUseFinancials(): Boolean {
        return capabilities.value.isFinancialEnabled
    }
    
    override fun canUseAnalytics(): Boolean {
        return capabilities.value.isAnalyticsEnabled
    }
    
    override fun canUseForecasting(): Boolean {
        return capabilities.value.isForecastingEnabled
    }

    override fun maxFlocks(): Int = capabilities.value.maxFlocks
    override fun maxBirdsPerFlock(): Int = capabilities.value.maxBirdsPerFlock
    override fun maxFlocklets(): Int = capabilities.value.maxFlocklets
    override fun allowedSpecies(): Set<com.example.hatchtracker.model.Species> = capabilities.value.allowedSpecies
    override fun maxActiveIncubations(): Int = capabilities.value.maxActiveIncubations
    override fun maxEggsPerIncubation(): Int = capabilities.value.maxEggsPerIncubation
    override fun maxChicksPerFlocklet(): Int = capabilities.value.maxChicksPerFlocklet
}

