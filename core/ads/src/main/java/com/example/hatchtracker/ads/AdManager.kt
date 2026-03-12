package com.example.hatchtracker.ads

import android.app.Activity
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages AdMob initialization and ad visibility logic.
 */
@Singleton
class AdManager @Inject constructor(
    private val subscriptionStateManager: SubscriptionStateManager,
    private val rewardManager: RewardManager
) {
    private var isInitialized = false
    private var rewardedAd: RewardedAd? = null

    /**
     * Observable flow indicating if ads should be shown.
     * Delegates to SubscriptionStateManager for the single source of truth.
     */
    val shouldShowAds: StateFlow<Boolean> = subscriptionStateManager.shouldShowAds

    /**
     * Initializes the Mobile Ads SDK.
     */
    fun initialize() {
        if (!isInitialized && shouldShowAds.value) {
            // Avoid non-visual context initialization on Android 14+ strict mode.
            // SDK initialization is deferred to first UI-context ad load.
            Logger.d(TAG, "AdMob initialization deferred until first UI-context ad request")
            isInitialized = true
        }
    }
    
    /**
     * Loads and shows a rewarded ad.
     * @param onRewardEarned Callback invoked when the user earns a reward.
     */
    fun loadAndShowRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Logger.e(TAG, "Rewarded ad failed to load: ${adError.message}")
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Logger.d(TAG, "Rewarded ad loaded.")
                rewardedAd = ad
                
                rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Logger.d(TAG, "Ad dismissed fullscreen content.")
                        rewardedAd = null
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Logger.e(TAG, "Ad failed to show fullscreen content.")
                        rewardedAd = null
                    }
                }
                
                rewardedAd?.show(activity, OnUserEarnedRewardListener { rewardItem ->
                    Logger.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                    rewardManager.grantAdFreeReward()
                    onRewardEarned()
                })
            }
        })
    }

    companion object {
        private const val TAG = "AdManager"
        
        // Test Ad Unit IDs
        const val BANNER_AD_UNIT_ID = "ca-app-pub-8101096950090429/2406433109" // Real Banner
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Test Rewarded
    }
}
