package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.AppAccessConfig
import com.example.hatchtracker.data.models.SubscriptionTier
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Observes the remote app access configuration from Firestore.
     * Emits safe defaults if the document or specific fields are missing.
     */
    fun observeAppAccessConfig(): Flow<AppAccessConfig> = callbackFlow {
        val docPath = "config/app_access"
        val subscription = firestore.collection("config")
            .document("app_access")
            .addSnapshotListener { snapshot, error ->
                com.example.hatchtracker.core.logging.FirebasePerfTracer.recordRead()
                val now = System.currentTimeMillis()
                if (error != null) {
                    val status = if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        com.example.hatchtracker.data.models.FetchStatus.PERMISSION_DENIED
                    } else {
                        com.example.hatchtracker.data.models.FetchStatus.ERROR
                    }
                    
                    com.example.hatchtracker.core.logging.Logger.e(
                        com.example.hatchtracker.core.logging.LogTags.DB,
                        "op=observeAppAccessConfig status=$status path=$docPath error=${error.message}"
                    )
                    trySend(AppAccessConfig(
                        fetchStatus = status,
                        fetchTimestamp = now,
                        errorMessage = error.message
                    ))
                    return@addSnapshotListener
                }

                val config = if (snapshot != null && snapshot.exists()) {
                    com.example.hatchtracker.core.logging.Logger.d(
                        com.example.hatchtracker.core.logging.LogTags.DB, 
                        "op=observeAppAccessConfig status=success path=$docPath"
                    )
                    val tierString = snapshot.getString("reviewerTier") ?: "FREE"
                    val tier = try {
                        SubscriptionTier.valueOf(tierString.uppercase())
                    } catch (e: Exception) {
                        SubscriptionTier.FREE
                    }

                    @Suppress("UNCHECKED_CAST")
                    val allowlist = snapshot.data?.get("reviewerAllowlistUids") as? List<String>
                    
                    val communityData = snapshot.get("communityConfig") as? Map<*, *>
                    val communityConfig = com.example.hatchtracker.data.models.CommunityConfig(
                        communityEnabled = communityData?.get("communityEnabled") as? Boolean ?: false,
                        feedEnabled = communityData?.get("feedEnabled") as? Boolean ?: false,
                        postingEnabled = communityData?.get("postingEnabled") as? Boolean ?: false,
                        commentsEnabled = communityData?.get("commentsEnabled") as? Boolean ?: false,
                        reactionsEnabled = communityData?.get("reactionsEnabled") as? Boolean ?: false,
                        reportingEnabled = communityData?.get("reportingEnabled") as? Boolean ?: false,
                        marketplaceEnabled = communityData?.get("marketplaceEnabled") as? Boolean ?: false,
                        marketplaceEquipmentEnabled = communityData?.get("marketplaceEquipmentEnabled") as? Boolean ?: false,
                        marketplaceEggSalesEnabled = communityData?.get("marketplaceEggSalesEnabled") as? Boolean ?: false,
                        marketplaceLiveAnimalSalesEnabled = communityData?.get("marketplaceLiveAnimalSalesEnabled") as? Boolean ?: false,
                        videoUploadEnabled = communityData?.get("videoUploadEnabled") as? Boolean ?: false,
                        directMessagesEnabled = communityData?.get("directMessagesEnabled") as? Boolean ?: false,
                        communityProjectsEnabled = communityData?.get("communityProjectsEnabled") as? Boolean ?: false,
                        communityExpertiseEnabled = communityData?.get("communityExpertiseEnabled") as? Boolean ?: false,
                        communityMarketplaceTrustEnabled = communityData?.get("communityMarketplaceTrustEnabled") as? Boolean ?: false,
                        communityQuestionRoutingEnabled = communityData?.get("communityQuestionRoutingEnabled") as? Boolean ?: false,
                        communityShareCardsEnabled = communityData?.get("communityShareCardsEnabled") as? Boolean ?: false,
                        moderationEnabled = communityData?.get("moderationEnabled") as? Boolean ?: false,
                        moderationQueueEnabled = communityData?.get("moderationQueueEnabled") as? Boolean ?: false,
                        marketplaceModerationEnabled = communityData?.get("marketplaceModerationEnabled") as? Boolean ?: false,
                        userBlockingEnabled = communityData?.get("userBlockingEnabled") as? Boolean ?: false,
                        strikeSystemEnabled = communityData?.get("strikeSystemEnabled") as? Boolean ?: false,
                        insightsEnabled = communityData?.get("insightsEnabled") as? Boolean ?: false
                    )

                    AppAccessConfig(
                        reviewerOverlayEnabled = snapshot.getBoolean("reviewerOverlayEnabled") ?: false,
                        reviewerAllowlistUids = allowlist ?: emptyList(),
                        reviewerTier = tier,
                        expiresAt = snapshot.getLong("expiresAt"),
                        lastUpdated = snapshot.getTimestamp("lastUpdated")?.seconds,
                        fetchStatus = com.example.hatchtracker.data.models.FetchStatus.SUCCESS,
                        fetchTimestamp = now,
                        communityConfig = communityConfig
                    )
                } else {
                    com.example.hatchtracker.core.logging.Logger.w(
                        com.example.hatchtracker.core.logging.LogTags.DB,
                        "op=observeAppAccessConfig status=missing path=$docPath"
                    )
                    AppAccessConfig(
                        fetchStatus = com.example.hatchtracker.data.models.FetchStatus.NOT_FOUND,
                        fetchTimestamp = now
                    )
                }
                
                trySend(config)
            }

        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()
}
