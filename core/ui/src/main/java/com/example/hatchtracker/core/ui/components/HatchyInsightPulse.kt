package com.example.hatchtracker.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A subtle, premium microinteraction that pulses a "presence" cue behind an avatar or icon.
 * Used for Hatchy's insight cards and presence indicators.
 */
@Composable
fun HatchyInsightPulse(
    insightId: String,
    forceReducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(1.0f) }
    val glowAlpha = remember { Animatable(0.0f) }

    LaunchedEffect(insightId) {
        if (insightId.isEmpty() || forceReducedMotion) return@LaunchedEffect
        
        // 120ms delay for a natural appearance
        kotlinx.coroutines.delay(120)

        // Scale: 1.0 -> 1.06 -> 1.0 (450ms)
        launch {
            scale.animateTo(
                targetValue = 1.06f,
                animationSpec = tween(durationMillis = 225, easing = FastOutSlowInEasing)
            )
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 225, easing = FastOutSlowInEasing)
            )
        }

        // Glow: Pulse with slower fade (600ms total)
        launch {
            glowAlpha.animateTo(
                targetValue = 0.15f,
                animationSpec = tween(durationMillis = 200)
            )
            glowAlpha.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(durationMillis = 400)
            )
        }
    }

    Box(contentAlignment = Alignment.Center) {
        // Radial Glow
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(scale.value)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = glowAlpha.value),
                    shape = CircleShape
                )
        )
        Box(modifier = Modifier.scale(scale.value)) {
            content()
        }
    }
}
