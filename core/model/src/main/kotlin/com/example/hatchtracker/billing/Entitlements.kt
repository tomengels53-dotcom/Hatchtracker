package com.example.hatchtracker.billing

import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.model.Species
import kotlinx.coroutines.flow.StateFlow

/**
 * Unified entitlements interface for checking subscription-based capabilities.
 * Placed in core:model so both core:data and core:billing can depend on it
 * without creating a module cycle.
 */
interface Entitlements {
    val tier: StateFlow<SubscriptionTier>
    val capabilities: StateFlow<SubscriptionCapabilities>

    fun canAccessPROIntelligence(): Boolean
    fun maxBreeds(): Int
    fun canUseSelectiveBreeding(): Boolean
    fun canLinkParentOffspring(): Boolean
    fun canUseFinancials(): Boolean
    fun canUseAnalytics(): Boolean
    fun canUseForecasting(): Boolean
    fun maxFlocks(): Int
    fun maxBirdsPerFlock(): Int
    fun maxFlocklets(): Int
    fun allowedSpecies(): Set<Species>
    fun maxActiveIncubations(): Int
    fun maxEggsPerIncubation(): Int
    fun maxChicksPerFlocklet(): Int
}

