package com.example.hatchtracker.core.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animates elevation smoothly with a max limit.
 *
 * Constraint: Elevation must never animate simultaneously with scale on the same container.
 * Keep motion minimal and controlled.
 */
@Composable
fun animatePremiumElevation(
    targetElevation: Dp,
    maxElevation: Dp = 2.dp
): State<Dp> {
    val coercedElevation = targetElevation.coerceAtMost(maxElevation)
    return animateDpAsState(
        targetValue = coercedElevation,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "PremiumElevation"
    )
}
