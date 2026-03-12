package com.example.hatchtracker.domain.util

import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.domain.model.SpeciesCatalog

/**
 * UI representation of a species row with locking information.
 */
data class SpeciesUiRow(
    val speciesId: String,
    val displayName: String,
    val isLocked: Boolean,
    val lockedReason: String?,
    val minTier: SubscriptionTier
)

/**
 * Centralized helper for species gating logic.
 * Used across all modules to ensure consistent tier enforcement.
 */
object SpeciesGatingHelper {
    
    /**
     * Check if a user can use a specific species based on their tier.
     * 
     * @param speciesId The species ID to check
     * @param tier The user's subscription tier
     * @param isAdminOrDeveloper Whether the user is an admin or developer (bypasses restrictions)
     * @return true if the species can be used, false otherwise
     */
    fun canUseSpecies(
        speciesId: String,
        tier: SubscriptionTier,
        isAdminOrDeveloper: Boolean
    ): Boolean {
        if (isAdminOrDeveloper) return true
        
        val species = SpeciesCatalog.getById(speciesId) ?: return false
        return tier.ordinal >= species.minTier.ordinal
    }
    
    /**
     * Get all species as UI rows with locking information.
     * 
     * @param tier The user's subscription tier
     * @param isAdminOrDeveloper Whether the user is an admin or developer
     * @return List of species UI rows with lock states
     */
    fun getSpeciesUiRows(
        tier: SubscriptionTier,
        isAdminOrDeveloper: Boolean
    ): List<SpeciesUiRow> {
        return SpeciesCatalog.ALL_SPECIES.map { species ->
            val isLocked = !canUseSpecies(species.id, tier, isAdminOrDeveloper)
            SpeciesUiRow(
                speciesId = species.id,
                displayName = species.displayName,
                isLocked = isLocked,
                lockedReason = if (isLocked) "Available on EXPERT & PRO" else null,
                minTier = species.minTier
            )
        }
    }
    
    /**
     * Get a user-friendly upgrade message for a locked species.
     * 
     * @param speciesName The display name of the species
     * @return A formatted message prompting upgrade
     */
    fun getUpgradeMessage(speciesName: String): String {
        return "$speciesName is available on EXPERT and PRO. Upgrade to unlock."
    }
}

