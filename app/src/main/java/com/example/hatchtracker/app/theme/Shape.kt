package com.example.hatchtracker.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Technical Agriculture Shapes: Semi-rounded, sturdy, utilitarian
// Adjusted for a slightly softer, more handcrafted feel while maintaining professionalism.
val Shapes = Shapes(
    // Small components (tags, chips, text fields) - Keep at 8.dp
    small = RoundedCornerShape(8.dp),

    // Medium components (cards, dialogs, buttons) - Increased for a softer feel
    medium = RoundedCornerShape(20.dp),

    // Large components (drawers, modal sheets) - Increased for a more premium look
    large = RoundedCornerShape(28.dp),
    
    // Full screen surfaces could use 0dp or very small radius - Keep at 24.dp
    extraLarge = RoundedCornerShape(24.dp)
)
