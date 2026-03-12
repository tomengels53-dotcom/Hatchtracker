package com.example.hatchtracker.app.ui

import com.example.hatchtracker.core.ui.R as UiR

/**
 * Guardrail for Splash assets to ensure we use supported drawables 
 * (PNG/JPG/WEBP/Vector) instead of adaptive mipmaps in Compose Image().
 */
object SplashAssets {
    @JvmField val WHOLE_EGG_RES: Int = com.example.hatchtracker.core.ui.R.drawable.whole_egg_removebg_preview
    @JvmField val CRACKED_EGG_RES: Int = com.example.hatchtracker.core.ui.R.drawable.cracked_egg1_final_removebg_preview
    @JvmField val DESIGNER_RES: Int = com.example.hatchtracker.core.ui.R.drawable.designer
}
