package com.example.hatchtracker.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * ColorSchemeExtensions: Bridges the gap between hardcoded semantic colors 
 * and the MaterialTheme system.
 */

val ColorScheme.profit: Color
    get() = primary // Stable Moss Green

val ColorScheme.loss: Color
    get() = error // Stable Red

val ColorScheme.warningAccent: Color
    get() = tertiary // Stable Gold
