package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.mappers.*
import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.domain.model.*
import com.example.hatchtracker.domain.repository.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MarketplaceListingRepository, MarketplaceComplianceEvaluator {

    private val LISTINGS_COLLECTION = "marketplace_listings"
    private val SALES_COLLECTION = "marketplace_sales"

    override fun getListings(category: String?, limit: Int): Flow<List<MarketplaceListing>> = callbackFlow {
        var query: Query = firestore.collection(LISTINGS_COLLECTION)
            .whereEqualTo("state", "ACTIVE")
            .whereEqualTo("moderationState", "ACTIVE")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (category != null) {
            query = query.whereEqualTo("category", category)
        }

        val subscription = query.limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val listings = snapshot?.documents?.mapNotNull { it.toObject(ListingDto::class.java)?.toDomain() } ?: emptyList()
                trySend(listings)
            }
        awaitClose { subscription.remove() }
    }

    override fun getListing(listingId: String): Flow<MarketplaceListing?> = callbackFlow {
        val subscription = firestore.collection(LISTINGS_COLLECTION)
            .document(listingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val listing = snapshot?.toObject(ListingDto::class.java)?.toDomain()
                trySend(listing)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createListing(listing: MarketplaceListing): Result<String> {
        return try {
            val id = listing.id.ifEmpty { java.util.UUID.randomUUID().toString() }
            firestore.collection(LISTINGS_COLLECTION)
                .document(id)
                .set(listing.toDto().copy(id = id))
                .await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateListing(listing: MarketplaceListing): Result<Unit> {
        return try {
            firestore.collection(LISTINGS_COLLECTION)
                .document(listing.id)
                .set(listing.toDto())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateModerationState(listingId: String, moderationState: ListingModerationState): Result<Unit> {
        return try {
            firestore.collection(LISTINGS_COLLECTION)
                .document(listingId)
                .update("moderationState", moderationState.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteListing(listingId: String): Result<Unit> {
        return try {
            firestore.collection(LISTINGS_COLLECTION).document(listingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordSale(sale: MarketplaceSale): Result<Unit> {
        return try {
            val saleId = sale.id.ifEmpty { java.util.UUID.randomUUID().toString() }
            firestore.collection(SALES_COLLECTION)
                .document(saleId)
                .set(sale.toDto().copy(id = saleId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun evaluate(listing: MarketplaceListing): ComplianceStatus {
        // Dark launch implementation: evaluate based on simple policies
        if (listing.category == ListingCategory.LIVE_BIRDS) {
            // Live birds require manual review or specific flags for now
            return ComplianceStatus.MANUAL_REVIEW_REQUIRED
        }
        return ComplianceStatus.ALLOWED
    }
}
