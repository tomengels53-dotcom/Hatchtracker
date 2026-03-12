package com.example.hatchtracker.billing

import android.app.Activity
import android.content.Context
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PendingPurchasesParams
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Play Billing connections and subscription queries.
 */
@Singleton
class BillingManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val functionsRepository: com.example.hatchtracker.data.repository.FunctionsRepository
) {
    companion object {
        private const val TAG = LogTags.BILLING
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private var billingClient: BillingClient? = null
    private var isConnecting = false


    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // Process purchases (null list is treated as 0 purchases)
            val purchaseList = purchases ?: emptyList()
            var highestRealTier = SubscriptionTier.FREE
            var highestRealProductId = ""
            
            for (purchase in purchaseList) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val isTest = purchase.orderId.isNullOrEmpty()
                    
                    for (productId in purchase.products) {
                        // Always acknowledge valid purchases to prevent refunds
                        if (!purchase.isAcknowledged) {
                            handlePurchase(purchase)
                        }

                        // Verify with Cloud Function (Server-Side Verification & Update)
                        if (!isTest) {
                            scope.launch {
                                Logger.d(TAG, "Verifying purchase for $productId...")
                                val result = functionsRepository.verifyPlaySubscription(
                                    purchaseToken = purchase.purchaseToken,
                                    productId = productId,
                                    packageName = context.packageName
                                )
                                if (result.isSuccess) {
                                    Logger.d(TAG, "Verification triggered for $productId")
                                } else {
                                    Logger.e(TAG, "Verification failed call for $productId", result.exceptionOrNull())
                                }
                            }
                        }
                    }
                }
            }
            
            // Legacy client-side sync logic removed.
            // Client now relies on Firestore updates from Cloud Functions.
            Logger.i(TAG, "Purchase processing complete. Waiting for server update...")
        }
    }

    /**
     * Acknowledges a purchase to make it permanent.
     */
    private fun handlePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
            
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Logger.e(TAG, "Acknowledge purchase failed: ${billingResult.debugMessage}")
            } else {
                Logger.d(TAG, "Purchase acknowledged successfully")
            }
        }
    }

    /**
     * Starts the billing connection.
     */
    fun startConnection() {
        if (billingClient?.isReady == true) {
            Logger.d(TAG, "Billing client is already ready.")
            return
        }
        if (isConnecting) {
            Logger.d(TAG, "Billing client connection is already in progress.")
            return
        }

        isConnecting = true

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
            
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryActiveSubscriptions()
                } else {
                    Logger.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                Logger.w(TAG, "Billing service disconnected")
                // Try to restart the connection on the next request.
            }
        })
    }
    
    /**
     * Queries for active subscriptions.
     */
    fun queryActiveSubscriptions() {
        if (billingClient?.isReady == true) {
            billingClient!!.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Re-use logic to update tier
                    purchasesUpdatedListener.onPurchasesUpdated(billingResult, purchases)
                } else {
                    Logger.e(TAG, "Query purchases failed: ${billingResult.debugMessage}")
                }
            }
        } else {
            Logger.e(TAG, "Billing client not ready")
        }
    }

    /**
     * Helper to query product details.
     */
    fun queryProductDetails(activity: Activity, productId: String) {
         val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                launchBillingFlow(activity, productDetails)
            } else {
                 Logger.e(TAG, "Product details not found for $productId")
            }
        }
    }

    /**
     * Launches the billing flow for a specific product.
     */
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken ?: ""
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
            
        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Alias for restoring purchases.
     */
    fun restorePurchases() {
        queryActiveSubscriptions()
    }

    /**
     * Test mode hook to force a tier sync without the Billing UI.
     * RESTRICTED: Only works if the user is a developer.
     */
    fun debugForceSync(tier: SubscriptionTier) {
        Logger.w(TAG, "debugForceSync is disabled. Use Cloud Functions for testing.")
    }

    /**
     * Ends the billing connection.
     */
    fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
        isConnecting = false
    }
}

