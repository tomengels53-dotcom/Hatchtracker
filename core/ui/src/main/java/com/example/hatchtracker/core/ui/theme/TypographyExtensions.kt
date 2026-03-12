package com.example.hatchtracker.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Semantic Data Emphasis (e.g. Scores, Alerts, Financial Totals)
 * Used universally across feature modules without explicit fontWeight overrides.
 */
val Typography.bodyEmphasis: TextStyle
    get() = bodyLarge.copy(fontWeight = FontWeight.SemiBold)
