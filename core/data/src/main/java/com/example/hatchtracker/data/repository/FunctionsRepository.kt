package com.example.hatchtracker.data.repository

import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.models.SubscriptionTier
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FunctionsRepository @Inject constructor(
    private val functions: FirebaseFunctions
) {

    data class VerifySubscriptionResponse(
        val subscriptionTier: SubscriptionTier,
        val adsEnabled: Boolean,
        val subscriptionExpiry: Long
    )

    /**
     * Calls the 'verifyPlaySubscription' Cloud Function.
     */
    suspend fun verifyPlaySubscription(
        purchaseToken: String,
        productId: String,
        packageName: String
    ): Result<VerifySubscriptionResponse> {
        return try {
            val data = hashMapOf(
                "purchaseToken" to purchaseToken,
                "productId" to productId,
                "packageName" to packageName
            )

            val result = functions
                .getHttpsCallable("verifyPlaySubscription")
                .call(data)
                .await()

            val responseData = result.data as? Map<*, *> ?: throw Exception("Invalid response format")
            
            val tierStr = responseData["subscriptionTier"] as? String ?: "free"
            val tier = try {
                SubscriptionTier.valueOf(tierStr.uppercase())
            } catch (e: Exception) {
                SubscriptionTier.FREE
            }
            
            val adsEnabled = responseData["adsEnabled"] as? Boolean ?: true
            val expiry = (responseData["subscriptionExpiry"] as? Number)?.toLong() ?: 0L

            Result.success(VerifySubscriptionResponse(tier, adsEnabled, expiry))
        } catch (e: Exception) {
            Logger.e(LogTags.BILLING, "Failed to verify subscription via Cloud Function", e)
            Result.failure(e)
        }
    }

    /**
     * Calls the 'deleteAccount' Cloud Function.
     */
    suspend fun deleteAccount(confirmString: String = "DELETE"): Result<Unit> {
        return try {
            val data = hashMapOf(
                "confirm" to confirmString
            )

            functions
                .getHttpsCallable("deleteAccount")
                .call(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(LogTags.AUTH, "Failed to delete account via Cloud Function", e)
            Result.failure(e)
        }
    }
}
