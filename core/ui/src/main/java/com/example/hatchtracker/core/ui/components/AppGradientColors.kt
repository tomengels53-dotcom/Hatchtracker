package com.example.hatchtracker.core.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppGradientColors(
    val screenTop: Color,
    val screenBottom: Color
)

val LocalAppGradientColors = staticCompositionLocalOf {
    AppGradientColors(
        screenTop = Color.Unspecified,
        screenBottom = Color.Unspecified
    )
}
