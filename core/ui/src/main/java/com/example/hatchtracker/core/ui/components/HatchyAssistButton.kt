package com.example.hatchtracker.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.R as UiR
import kotlinx.coroutines.launch

@Composable
fun HatchyAssistButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 20.dp,
    nudgeKey: Any? = null,
    forceReducedMotion: Boolean = false
) {
    val scale = remember { Animatable(1.0f) }
    val haloAlpha = remember { Animatable(0.0f) }

    LaunchedEffect(nudgeKey) {
        if (nudgeKey == null || forceReducedMotion) return@LaunchedEffect
        
        // Reduced motion check (Compose automatically sets duration to 0 if disabled, 
        // but we can be explicit if needed. Here we follow standard premium cue)
        
        // Nudge: 1.0 -> 1.06 -> 1.0
        launch {
            scale.animateTo(
                targetValue = 1.06f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        }
        
        // Halo: Fade in -> Fade out
        launch {
            haloAlpha.animateTo(
                targetValue = 0.3f,
                animationSpec = tween(durationMillis = 200)
            )
            haloAlpha.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    Box(
        modifier = modifier
            .padding(start = 20.dp, bottom = bottomPadding)
            .size(72.dp), // Slightly larger container for halo bounds
        contentAlignment = Alignment.Center
    ) {
        // Subtle Halo
        Box(
            modifier = Modifier
                .size(68.dp)
                .scale(scale.value)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = haloAlpha.value),
                    shape = CircleShape
                )
        )

        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(60.dp)
                .scale(scale.value)
                .testTag("HatchyAssistButton")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = UiR.drawable.hatchy_1),
                    contentDescription = "Ask Hatchy",
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}
