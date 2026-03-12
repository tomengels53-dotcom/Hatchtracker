@file:android.annotation.SuppressLint("MissingPermission")

package com.example.hatchtracker.core.ui.composeutil

import android.content.Context
import com.google.android.gms.ads.AdView

/**
 * Helpers for composing AdView objects.
 */
object AdViewFactory {
    fun createBannerAdView(context: Context, adUnitId: String): AdView {
        return AdView(context).apply {
            setAdSize(com.google.android.gms.ads.AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 360))
            this.adUnitId = adUnitId
            adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdClicked() {}
                override fun onAdClosed() {}
                override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {}
                override fun onAdImpression() {}
                override fun onAdLoaded() {}
                override fun onAdOpened() {}
            }
            loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
        }
    }
}
