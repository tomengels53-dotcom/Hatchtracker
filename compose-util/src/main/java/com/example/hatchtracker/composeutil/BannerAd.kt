package com.example.hatchtracker.composeutil

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView

/**
 * A helper composable that wraps an AdView.
 */
@Composable
fun BannerAd(adView: AdView, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { adView },
        modifier = modifier
    )
}
