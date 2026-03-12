package com.example.hatchtracker.billing

import com.example.hatchtracker.data.models.SubscriptionTier

/**
 * Immutable model defining the capabilities and limits for a subscription tier.
 */
data class SubscriptionCapabilities(
    val tier: SubscriptionTier,
    val maxActiveIncubations: Int,
    val maxEggsPerIncubation: Int,
    val maxFlocks: Int,
    val maxBirdsPerFlock: Int,
    val maxFlocklets: Int,
    val maxChicksPerFlocklet: Int,
    val allowedSpecies: Set<com.example.hatchtracker.model.Species>,
    val isParentOffspringLinkingEnabled: Boolean,
    val isSelectiveBreedingEnabled: Boolean,
    val isAdsEnabled: Boolean,
    val canExportData: Boolean,
    val isFinancialEnabled: Boolean,
    val isAnalyticsEnabled: Boolean,
    val isForecastingEnabled: Boolean,
    val isPricingStrategyEnabled: Boolean,
    val maxBreedsPerBatch: Int,
    
    // Equipment Bucket Limits (null = unlimited)
    val maxIncubationEquipment: Int?,
    val maxBroodingEquipment: Int?,
    val maxHousingEquipment: Int?,
    val maxCareEquipment: Int?,
    val maxMonitoringEquipment: Int?
) {
    companion object {
        private val ALL_SPECIES = com.example.hatchtracker.model.Species.values().toSet()
        private val BASIC_SPECIES = setOf(
            com.example.hatchtracker.model.Species.CHICKEN,
            com.example.hatchtracker.model.Species.DUCK,
            com.example.hatchtracker.model.Species.GOOSE,
            com.example.hatchtracker.model.Species.TURKEY
        )

        private val FREE_CAPS = SubscriptionCapabilities(
            tier = SubscriptionTier.FREE,
            maxActiveIncubations = 3,
            maxEggsPerIncubation = 20,
            maxFlocks = 5,
            maxBirdsPerFlock = 5,
            maxFlocklets = 5,
            maxChicksPerFlocklet = 20,
            allowedSpecies = BASIC_SPECIES,
            isParentOffspringLinkingEnabled = true,
            isSelectiveBreedingEnabled = false,
            isAdsEnabled = true,
            canExportData = false,
            isFinancialEnabled = false,
            isAnalyticsEnabled = false,
            isForecastingEnabled = false,
            isPricingStrategyEnabled = false,
            maxBreedsPerBatch = 1,
            maxIncubationEquipment = 1,
            maxBroodingEquipment = 2,
            maxHousingEquipment = 1,
            maxCareEquipment = 5,
            maxMonitoringEquipment = 2
        )

        private val EXPERT_CAPS = SubscriptionCapabilities(
            tier = SubscriptionTier.EXPERT,
            maxActiveIncubations = 5,
            maxEggsPerIncubation = 50,
            maxFlocks = 10,
            maxBirdsPerFlock = 10,
            maxFlocklets = 10,
            maxChicksPerFlocklet = 50,
            allowedSpecies = ALL_SPECIES,
            isParentOffspringLinkingEnabled = true,
            isSelectiveBreedingEnabled = false,
            isAdsEnabled = false,
            canExportData = false,
            isFinancialEnabled = true,
            isAnalyticsEnabled = true,
            isForecastingEnabled = true,
            isPricingStrategyEnabled = false,
            maxBreedsPerBatch = 5,
            maxIncubationEquipment = 5,
            maxBroodingEquipment = 10,
            maxHousingEquipment = 5,
            maxCareEquipment = 20,
            maxMonitoringEquipment = 10
        )

        private val PRO_CAPS = SubscriptionCapabilities(
            tier = SubscriptionTier.PRO,
            maxActiveIncubations = Int.MAX_VALUE,
            maxEggsPerIncubation = Int.MAX_VALUE,
            maxFlocks = Int.MAX_VALUE,
            maxBirdsPerFlock = Int.MAX_VALUE,
            maxFlocklets = Int.MAX_VALUE,
            maxChicksPerFlocklet = Int.MAX_VALUE,
            allowedSpecies = ALL_SPECIES,
            isParentOffspringLinkingEnabled = true,
            isSelectiveBreedingEnabled = true,
            isAdsEnabled = false,
            canExportData = true,
            isFinancialEnabled = true,
            isAnalyticsEnabled = true,
            isForecastingEnabled = true,
            isPricingStrategyEnabled = true,
            maxBreedsPerBatch = 5,
            maxIncubationEquipment = null,
            maxBroodingEquipment = null,
            maxHousingEquipment = null,
            maxCareEquipment = null,
            maxMonitoringEquipment = null
        )

        /**
         * Returns the capabilities for a specific tier.
         */
        fun getForTier(tier: SubscriptionTier): SubscriptionCapabilities {
            return when (tier) {
                SubscriptionTier.FREE -> FREE_CAPS
                SubscriptionTier.EXPERT -> EXPERT_CAPS
                SubscriptionTier.PRO -> PRO_CAPS
            }
        }
    }
}


