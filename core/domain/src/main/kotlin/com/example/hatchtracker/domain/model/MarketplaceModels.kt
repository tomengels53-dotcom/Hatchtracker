package com.example.hatchtracker.domain.model

/**
 * Represents a listing in the marketplace.
 */
data class MarketplaceListing(
    val id: String = "",
    val sellerUserId: String,
    val sellerSnapshot: MarketplaceSellerSnapshot,
    val category: ListingCategory,
    val title: String,
    val description: String,
    val price: Double,
    val currency: String = "USD",
    val media: List<CommunityMedia> = emptyList(),
    val linkedEntities: List<EntityPassportSnapshot> = emptyList(),
    val state: ListingState = ListingState.DRAFT,
    val complianceStatus: ComplianceStatus = ComplianceStatus.PENDING,
    val moderationState: ListingModerationState = ListingModerationState.ACTIVE,
    val pickupRegion: String? = null,
    val willingToShip: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ListingCategory {
    EQUIPMENT, ACCESSORIES, EGGS, LIVE_BIRDS, SERVICES
}

enum class ListingState {
    DRAFT, PENDING_REVIEW, ACTIVE, PAUSED, SOLD, WITHDRAWN, BLOCKED
}

enum class ListingModerationState {
    ACTIVE,
    MODERATION_HOLD,
    REMOVED,
    BLOCKED,
    UNDER_REVIEW
}

enum class ComplianceStatus {
    PENDING, ALLOWED, ALLOWED_WITH_CONDITIONS, MANUAL_REVIEW_REQUIRED, BLOCKED
}

/**
 * Represents a completed marketplace sale.
 */
data class MarketplaceSale(
    val id: String = "",
    val listingId: String,
    val sellerUserId: String,
    val buyerUserId: String,
    val amount: Double,
    val currency: String = "USD",
    val linkedEntities: List<EntityReference> = emptyList(),
    val completedAt: Long = System.currentTimeMillis()
)

data class EntityReference(
    val entityType: String,
    val entityId: String
)
