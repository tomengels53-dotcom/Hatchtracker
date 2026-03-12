package com.example.hatchtracker.domain.model
 
import com.example.hatchtracker.data.models.SubscriptionTier

/**
 * Represents a user's profile and subscription status.
 */
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val profilePictureUrl: String? = null,
    val email: String = "",
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val subscriptionSource: String = "none",
    val subscriptionProductId: String = "",
    val subscriptionActive: Boolean = false,
    val lastSubscriptionSync: Long = 0,
    val lastSyncStatus: String? = null,
    val downgradeReason: String? = null,
    val adsEnabled: Boolean = true,
    val isDeveloper: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // Localization Settings
    val countryCode: String = "", // ISO 3166-1 alpha-2
    val currencyCode: String = "USD", // ISO 4217
    val weightUnit: String = "kg", // Default to metric
    val dateFormat: String = "DD-MM-YYYY",
    val timeFormat: String = "24h",
    
    // Roles
    val roles: List<String> = emptyList(),
    val isSystemAdmin: Boolean = false,
    val isCommunityAdmin: Boolean = false,
    
    // Legal Consent Tracking
    val termsVersionAccepted: String = "",
    val privacyVersionAccepted: String = "",
    val consentTimestamp: Long = 0,

    val reputation: Int = 0,
    val preferredLanguage: String = "",

    // --- Community Identity Basics ---
    val username: String = "",
    val bio: String = "",
    val publicProfileEnabled: Boolean = false,

    // --- Breeder Identity ---
    val breederType: String = "", // e.g. Hobbyist, Professional, Preservationist
    val speciesFocus: List<String> = emptyList(),
    val specialties: List<String> = emptyList(),

    // --- Privacy & Sharing ---
    val showRegionPublicly: Boolean = false,
    val allowDirectMessages: Boolean = false,
    val allowMarketplaceContact: Boolean = false,

    // --- Marketplace Readiness ---
    val marketplaceSellerEnabled: Boolean = false,
    val pickupRegion: String = "",
    val willingToShip: Boolean = false,

    // --- System / Backend Managed ---
    val communityProfileVersion: Int = 1,
    val moderationStatus: String = "CLEAN", // CLEAN, WARNING, STRIKE, BANNED
    val reputationSummary: String = "",
    val sellerVerificationStatus: String = "UNVERIFIED", // UNVERIFIED, PENDING, VERIFIED
    val profileCompletenessScore: Int = 0
)
