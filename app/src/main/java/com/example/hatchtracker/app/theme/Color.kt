package com.example.hatchtracker.app.theme

import androidx.compose.ui.graphics.Color

// RUSTIC MODERN PREMIUM POULTRY AESTHETIC PALETTE

// --- LIGHT THEME TOKENS ---
val parchmentSurface = Color(0xFFF4EFE6) // Main background
val warmCard = Color(0xFFF1E7D6)        // Card surface (warm)
val barnBrown = Color(0xFF8C5A2B)       // Accent / Secondary
val mossGreen = Color(0xFF2F5D3A)       // Primary
val forestAccent = Color(0xFF1F3E2A)    // Darker accent

// Mapping to Material Slots (Light)
val md_theme_light_primary = mossGreen
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFD4E8D1) // Keep soft sage container
val md_theme_light_onPrimaryContainer = forestAccent

val md_theme_light_secondary = barnBrown
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFF4D8C1)
val md_theme_light_onSecondaryContainer = Color(0xFF321404)

val md_theme_light_tertiary = Color(0xFF917C2F) // Harvest Gold (Keep)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFF8E7AD)
val md_theme_light_onTertiaryContainer = Color(0xFF2D2300)

val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)

val md_theme_light_background = parchmentSurface
val md_theme_light_onBackground = Color(0xFF1A1C19)
val md_theme_light_surface = parchmentSurface
val md_theme_light_onSurface = Color(0xFF1A1C19)
val md_theme_light_surfaceVariant = Color(0xFFEBEFDA) // Warm Grey-Green
val md_theme_light_onSurfaceVariant = Color(0xFF43493F)
val md_theme_light_outline = Color(0xFF73796E)

// --- DARK THEME TOKENS ---
val forestNightBackground = Color(0xFF0D1A14) // Deep Forest Night (Corrected Hex)
val forestNightBaseLayer = Color(0xFF12261D)  // New Base Layer
val forestNightElevated = Color(0xFF183428)   // Elevated Layer
val forestNightHero = Color(0xFF1E3E2E)       // Hero Layer
val forestNightSurface = Color(0xFF14261C)    // General Surface

// Mapping to Material Slots (Dark)
val md_theme_dark_primary = Color(0xFF98D1A0) // Soft Moss Green
val md_theme_dark_onPrimary = Color(0xFF003914)
val md_theme_dark_primaryContainer = Color(0xFF155225)
val md_theme_dark_onPrimaryContainer = Color(0xFFB3EDBA)

val md_theme_dark_secondary = Color(0xFFE6BEA0) // Muted Sandstone
val md_theme_dark_onSecondary = Color(0xFF442B15)
val md_theme_dark_secondaryContainer = Color(0xFF5D4129)
val md_theme_dark_onSecondaryContainer = Color(0xFFFFDCC7)

val md_theme_dark_tertiary = Color(0xFFDCC67E) // Muted Hay
val md_theme_dark_onTertiary = Color(0xFF3B2F00)
val md_theme_dark_tertiaryContainer = Color(0xFF53450E)
val md_theme_dark_onTertiaryContainer = Color(0xFFF9E297)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_theme_dark_background = forestNightBackground
val md_theme_dark_onBackground = Color(0xFFE2E3DD)
val md_theme_dark_surface = forestNightSurface
val md_theme_dark_onSurface = Color(0xFFE2E3DD)
val md_theme_dark_surfaceVariant = Color(0xFF2A362D) // Mossy dark grey
val md_theme_dark_onSurfaceVariant = Color(0xFFC3C8BC)
val md_theme_dark_outline = Color(0xFF505A51) // Subdued olive grey

// --- SEMANTIC FINANCE COLORS ---
val warmProfit = Color(0xFF6FAF72) // Warm Green (User Spec)
val warmLoss = Color(0xFFD27C6E)   // Warm Red (User Spec)

val financePositive = warmProfit
val financeNegative = warmLoss

// --- GRADIENTS / TEXTURES ---
val ScreenGradientTop = mossGreen.copy(alpha = 0.06f)
val ScreenGradientBottom = Color.Transparent

val ScreenGradientTopDark = mossGreen.copy(alpha = 0.06f) // Same subtle tint for dark
val ScreenGradientBottomDark = Color.Transparent

// --- INTERACTION ---
val rippleColor = mossGreen
