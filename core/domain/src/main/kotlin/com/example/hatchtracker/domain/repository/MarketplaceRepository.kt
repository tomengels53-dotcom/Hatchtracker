package com.example.hatchtracker.domain.repository

import com.example.hatchtracker.domain.model.MarketplaceListing
import com.example.hatchtracker.domain.model.MarketplaceSale
import com.example.hatchtracker.domain.model.ComplianceStatus
import com.example.hatchtracker.domain.model.ListingModerationState
import kotlinx.coroutines.flow.Flow

/**
 * Repository for marketplace listing and lifecycle operations.
 */
interface MarketplaceListingRepository {
    fun getListings(category: String? = null, limit: Int = 20): Flow<List<MarketplaceListing>>
    fun getListing(listingId: String): Flow<MarketplaceListing?>
    suspend fun createListing(listing: MarketplaceListing): Result<String>
    suspend fun updateListing(listing: MarketplaceListing): Result<Unit>
    suspend fun updateModerationState(listingId: String, moderationState: ListingModerationState): Result<Unit>
    suspend fun deleteListing(listingId: String): Result<Unit>
    
    // Sales
    suspend fun recordSale(sale: MarketplaceSale): Result<Unit>
}

/**
 * Evaluates listing compliance based on policy.
 */
interface MarketplaceComplianceEvaluator {
    suspend fun evaluate(listing: MarketplaceListing): ComplianceStatus
}
