package com.example.hatchtracker.domain.model

/**
 * Lightweight, privacy-safe projection of a user profile for social context.
 * Embedded in posts and comments for historical stability.
 */
data class CommunityAuthorSnapshot(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val breederType: String?,
    val reputationScore: Int,
    val snapshotVersion: Int = 1,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Lightweight projection of a user profile for marketplace seller context.
 * Embedded in listings for historical stability.
 */
data class MarketplaceSellerSnapshot(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val sellerVerificationStatus: String,
    val pickupRegion: String?,
    val willingToShip: Boolean,
    val sellerRating: Float = 0f,
    val snapshotVersion: Int = 1,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Factory for creating projections from a canonical UserProfile.
 */
object ProfileProjectionFactory {
    fun createAuthorSnapshot(profile: UserProfile): CommunityAuthorSnapshot {
        return CommunityAuthorSnapshot(
            userId = profile.userId,
            username = profile.username.ifEmpty { profile.displayName },
            displayName = profile.displayName,
            avatarUrl = profile.profilePictureUrl,
            breederType = if (profile.publicProfileEnabled) profile.breederType else null,
            reputationScore = profile.reputation
        )
    }

    fun createSellerSnapshot(profile: UserProfile): MarketplaceSellerSnapshot {
        return MarketplaceSellerSnapshot(
            userId = profile.userId,
            username = profile.username.ifEmpty { profile.displayName },
            displayName = profile.displayName,
            avatarUrl = profile.profilePictureUrl,
            sellerVerificationStatus = profile.sellerVerificationStatus,
            pickupRegion = if (profile.showRegionPublicly) profile.pickupRegion else null,
            willingToShip = profile.willingToShip
        )
    }

    fun createHatchySnapshot(): CommunityAuthorSnapshot {
        return CommunityAuthorSnapshot(
            userId = "system-hatchy",
            username = "hatchy",
            displayName = "Hatchy",
            avatarUrl = null, // System-handled or themed
            breederType = "Assistant",
            reputationScore = 9999
        )
    }
}
