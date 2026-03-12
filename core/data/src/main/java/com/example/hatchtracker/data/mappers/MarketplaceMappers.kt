package com.example.hatchtracker.data.mappers

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.domain.model.*

fun ListingDto.toDomain(): MarketplaceListing {
    return MarketplaceListing(
        id = id,
        sellerUserId = sellerUserId,
        sellerSnapshot = sellerSnapshot ?: MarketplaceSellerSnapshot(userId = sellerUserId, username = "Unknown", displayName = "Unknown", avatarUrl = null, sellerVerificationStatus = "UNVERIFIED", pickupRegion = null, willingToShip = false),
        category = ListingCategory.valueOf(category),
        title = title,
        description = description,
        price = price,
        currency = currency,
        media = media,
        linkedEntities = linkedEntities,
        state = ListingState.valueOf(state),
        complianceStatus = ComplianceStatus.valueOf(complianceStatus),
        moderationState = ListingModerationState.valueOf(moderationState),
        pickupRegion = pickupRegion,
        willingToShip = willingToShip,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun MarketplaceListing.toDto(): ListingDto {
    return ListingDto(
        id = id,
        sellerUserId = sellerUserId,
        sellerSnapshot = sellerSnapshot,
        category = category.name,
        title = title,
        description = description,
        price = price,
        currency = currency,
        media = media,
        linkedEntities = linkedEntities,
        state = state.name,
        complianceStatus = complianceStatus.name,
        moderationState = moderationState.name,
        pickupRegion = pickupRegion,
        willingToShip = willingToShip,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SaleDto.toDomain(): MarketplaceSale {
    return MarketplaceSale(
        id = id,
        listingId = listingId,
        sellerUserId = sellerUserId,
        buyerUserId = buyerUserId,
        amount = amount,
        currency = currency,
        linkedEntities = linkedEntities,
        completedAt = completedAt
    )
}

fun MarketplaceSale.toDto(): SaleDto {
    return SaleDto(
        id = id,
        listingId = listingId,
        sellerUserId = sellerUserId,
        buyerUserId = buyerUserId,
        amount = amount,
        currency = currency,
        linkedEntities = linkedEntities,
        completedAt = completedAt
    )
}
