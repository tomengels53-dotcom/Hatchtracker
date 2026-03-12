package com.example.hatchtracker.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.example.hatchtracker.billing.BillingMapping
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.core.ui.R
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Repository handling all billing-related logic.
 *
 * Responsibilities:
 * 1. Connect to Google Play Billing
 * 2. Query available products (Expert/Pro IDs)
 * 3. Handle purchase updates
 * 4. Verify purchases securely via Cloud Functions
 */
@Singleton
class BillingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : PurchasesUpdatedListener {

    private val TAG = LogTags.BILLING
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()

    // -- State --
    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    private val _activeSubscription = MutableStateFlow(false)
    val activeSubscription: StateFlow<Boolean> = _activeSubscription.asStateFlow()

    private val _subscriptionTier = MutableStateFlow<SubscriptionTier>(SubscriptionTier.FREE)
    val subscriptionTier: StateFlow<SubscriptionTier> = _subscriptionTier.asStateFlow()

    private val _activeSubscriptionProductId = MutableStateFlow<String?>(null)
    val activeSubscriptionProductId: StateFlow<String?> = _activeSubscriptionProductId.asStateFlow()

    private val _lastPlaySyncEpochMs = MutableStateFlow(0L)
    val lastPlaySyncEpochMs: StateFlow<Long> = _lastPlaySyncEpochMs.asStateFlow()

    private val _purchaseEvents = Channel<BillingEvent>(Channel.BUFFERED)
    val purchaseEvents = _purchaseEvents.receiveAsFlow()
    
    private var isConnected = false

    fun startConnection() {
        Logger.d(TAG, "Starting Billing Connection...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    Logger.d(TAG, "Billing Connection Established")
                    queryAvailableProducts()
                    queryActivePurchases() // Restore purchases / check history
                } else {
                    Logger.e(TAG, "Billing Setup Failed: ${billingResult.debugMessage}")
                    _purchaseEvents.trySend(BillingEvent.Error(UiText.StringResource(R.string.billing_error_setup_failed, billingResult.debugMessage)))
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                Logger.w(TAG, "Billing Service Disconnected. Retrying in 2s...")
                // Simple retry mechanism could be robustified with exponential backoff
            }
        })
    }

    /**
     * Queries product details for all known tiers.
     */
    private fun queryAvailableProducts() {
        val productList = listOf(
            // Expert
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingMapping.PRODUCT_EXPERT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingMapping.PRODUCT_EXPERT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            // Pro
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingMapping.PRODUCT_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingMapping.PRODUCT_PRO_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, detailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = detailsList
                Logger.d(TAG, "Products Loaded: ${detailsList.size}")
            } else {
                Logger.e(TAG, "Failed to load products: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * Launches the billing flow for a selected product.
     */
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseEvents.trySend(BillingEvent.Error(UiText.StringResource(R.string.billing_error_launch_failed, billingResult.debugMessage)))
        }
    }

    /**
     * Callback from Google Play.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _lastPlaySyncEpochMs.value = System.currentTimeMillis()
            updateLocalTierFromPurchases(purchases)
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Logger.i(TAG, "User canceled purchase.")
            _purchaseEvents.trySend(BillingEvent.Message(UiText.StringResource(R.string.billing_msg_purchase_canceled)))
        } else {
            Logger.e(TAG, "Purchase Error: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            _purchaseEvents.trySend(BillingEvent.Error(UiText.StringResource(R.string.billing_error_purchase_failed, billingResult.debugMessage)))
        }
    }

    /**
     * Handles a potentially successful purchase.
     * 1. Acknowledges it (if needed).
     * 2. Calls Cloud Function to verify and grant entitlement.
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Logger.d(TAG, "Purchase Acknowledged.")
                        verifyWithBackend(purchase)
                    } else {
                        Logger.e(TAG, "Acknowledge Failed: ${billingResult.debugMessage}")
                    }
                }
            } else {
                verifyWithBackend(purchase)
            }
        }
    }

    /**
     * Calls the Cloud Function `verifyPlaySubscription` to securely update the user's tier.
     */
    private fun verifyWithBackend(purchase: Purchase) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Logger.e(TAG, "No user logged in, cannot verify purchase.")
            return
        }

        // Prepare data
        val productId = purchase.products.firstOrNull() ?: return
        val data = hashMapOf(
            "purchaseToken" to purchase.purchaseToken,
            "productId" to productId,
            "packageName" to context.packageName // "com.example.hatchtracker"
        )

        Logger.d(TAG, "Verifying purchase with backend for $productId...")
        _purchaseEvents.trySend(BillingEvent.Loading(true))

        functions
            .getHttpsCallable("verifyPlaySubscription")
            .call(data)
            .addOnSuccessListener { result ->
                Logger.d(TAG, "Backend Verification Success: ${result.data}")
                _purchaseEvents.trySend(BillingEvent.Success)
                _purchaseEvents.trySend(BillingEvent.Loading(false))
            }
            .addOnFailureListener { e ->
                Logger.e(TAG, "Backend Verification Failed", e)
                _purchaseEvents.trySend(BillingEvent.Error(UiText.StringResource(R.string.billing_error_verification_failed, e.message ?: "")))
                _purchaseEvents.trySend(BillingEvent.Loading(false))
            }
    }
    
    /**
     * Checks for existing purchases (e.g. on app restart).
     */
    fun queryActivePurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
             _lastPlaySyncEpochMs.value = System.currentTimeMillis()
             if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                 updateLocalTierFromPurchases(purchases)
                 purchases.forEach { handlePurchase(it) }
             } else {
                 _activeSubscription.value = false
                 _subscriptionTier.value = SubscriptionTier.FREE
                 _activeSubscriptionProductId.value = null
             }
        }
    }

    private fun updateLocalTierFromPurchases(purchases: List<Purchase>) {
        val activePurchases = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        
        if (activePurchases.isEmpty()) {
            _activeSubscription.value = false
            _subscriptionTier.value = SubscriptionTier.FREE
            _activeSubscriptionProductId.value = null
            Logger.d(TAG, "No active local purchases found.")
            return
        }

        _activeSubscription.value = true
        
        // Find the highest tier
        var maxTier = SubscriptionTier.FREE
        var maxProductId: String? = null
        for (purchase in activePurchases) {
            for (productId in purchase.products) {
                val tier = BillingMapping.getTierForProduct(productId)
                if (tier.ordinal > maxTier.ordinal) {
                    maxTier = tier
                    maxProductId = productId
                }
            }
        }
        _subscriptionTier.value = maxTier
        _activeSubscriptionProductId.value = maxProductId
        Logger.d(TAG, "Local active tier updated to: $maxTier")
    }

    sealed class BillingEvent {
        object Success : BillingEvent()
        data class Message(val message: UiText) : BillingEvent()
        data class Error(val message: UiText) : BillingEvent()
        data class Loading(val isLoading: Boolean) : BillingEvent()
    }
}
