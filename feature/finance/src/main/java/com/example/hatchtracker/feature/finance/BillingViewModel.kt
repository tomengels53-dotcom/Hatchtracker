package com.example.hatchtracker.feature.finance

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.example.hatchtracker.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository
) : ViewModel() {

    val currencyCode: StateFlow<String> = userRepository.userProfile
        .map { it?.currencyCode ?: "USD" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    init {
        billingRepository.startConnection()
    }

    fun retry() {
        billingRepository.startConnection()
    }

    val products: StateFlow<List<ProductDetails>> = billingRepository.productDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val billingEvents = billingRepository.purchaseEvents
    
    // Connect to SubscriptionStatemanager which handles entitlement logic (Developer, Free, Pro)
    val currentTier = subscriptionStateManager.effectiveTier
    val activePlayProductId = subscriptionStateManager.activePlayProductId
    val lastPlaySyncEpochMs = subscriptionStateManager.lastPlaySyncEpochMs

    /**
     * Launches the purchase flow for a given product and offer.
     */
    fun buyProduct(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        billingRepository.launchBillingFlow(activity, productDetails, offerToken)
    }

    /**
     * Helper to find best offer token (simplified).
     */
    fun getOfferToken(productDetails: ProductDetails): String {
        return productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
    }

    fun restorePurchases() {
        billingRepository.queryActivePurchases()
    }
}



