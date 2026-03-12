package com.example.hatchtracker.data.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.example.hatchtracker.domain.model.*

@IgnoreExtraProperties
data class ListingDto(
    val id: String = "",
    val sellerUserId: String = "",
    val sellerSnapshot: MarketplaceSellerSnapshot? = null,
    val category: String = "EQUIPMENT",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String = "USD",
    val media: List<CommunityMedia> = emptyList(),
    val linkedEntities: List<EntityPassportSnapshot> = emptyList(),
    val state: String = "DRAFT",
    val complianceStatus: String = "PENDING",
    val moderationState: String = "ACTIVE",
    val pickupRegion: String? = null,
    val willingToShip: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@IgnoreExtraProperties
data class SaleDto(
    val id: String = "",
    val listingId: String = "",
    val sellerUserId: String = "",
    val buyerUserId: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val linkedEntities: List<EntityReference> = emptyList(),
    val completedAt: Long = 0
)
