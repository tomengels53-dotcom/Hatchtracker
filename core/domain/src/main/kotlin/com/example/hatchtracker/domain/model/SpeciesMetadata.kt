package com.example.hatchtracker.domain.model

import com.example.hatchtracker.data.models.SubscriptionTier

/**
 * Metadata for a species including tier requirements.
 */
data class SpeciesMetadata(
    val id: String,
    val displayName: String,
    val minTier: SubscriptionTier = SubscriptionTier.FREE
)

/**
 * Centralized catalog of all supported species with tier requirements.
 */
object SpeciesCatalog {
    val ALL_SPECIES = listOf(
        SpeciesMetadata("chicken", "Chicken", SubscriptionTier.FREE),
        SpeciesMetadata("duck", "Duck", SubscriptionTier.FREE),
        SpeciesMetadata("goose", "Goose", SubscriptionTier.FREE),
        SpeciesMetadata("turkey", "Turkey", SubscriptionTier.FREE),
        SpeciesMetadata("peafowl", "Peafowl", SubscriptionTier.EXPERT),
        SpeciesMetadata("pheasant", "Pheasant", SubscriptionTier.EXPERT),
        SpeciesMetadata("quail", "Quail", SubscriptionTier.EXPERT)
    )
    
    /**
     * Get species metadata by ID (case-insensitive).
     */
    fun getById(id: String): SpeciesMetadata? = 
        ALL_SPECIES.find { it.id.equals(id, ignoreCase = true) }
    
    /**
     * Get species metadata by display name (case-insensitive).
     */
    fun getByName(name: String): SpeciesMetadata? = 
        ALL_SPECIES.find { it.displayName.equals(name, ignoreCase = true) }
}
