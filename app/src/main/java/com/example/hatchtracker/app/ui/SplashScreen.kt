package com.example.hatchtracker.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.R
import com.example.hatchtracker.core.ui.R as UiR
import kotlinx.coroutines.delay

/**
 * DEVELOPER NOTE: When changing the SplashAssets API or resource references, 
 * uninstall the app from the device/emulator before re-running to ensure 
 * stale Dex overlays (from Apply Changes/Hot Reload) are cleared.
 * adb uninstall com.example.hatchtracker
 */
@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    var isCracking by remember { mutableStateOf(false) }
    var isHatched by remember { mutableStateOf(false) }
    
    // Shake Animation
    val shake = remember { Animatable(0f) }
    
    // Scale Animation (Pulse)
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: Idle / Pulse
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000), // Slower pulse
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(Unit) {
        delay(1000) // Longer initial delay
        
        // Phase 2: Shake/Crack
        isCracking = true
        shake.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 2000 // Slower shake
                0f at 0
                -5f at 200
                5f at 400
                -10f at 600
                10f at 800
                -5f at 1000
                5f at 1200
                0f at 2000
            }
        )
        
        // Phase 3: Hatch / Pop
        isHatched = true
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
        
        delay(2000) // Hold final state longer
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5DBB4C)),
        contentAlignment = Alignment.Center
    ) {
        // Determine which image to show based on state
        val imageRes = when {
            isHatched -> SplashAssets.DESIGNER_RES
            isCracking -> SplashAssets.CRACKED_EGG_RES
            else -> SplashAssets.WHOLE_EGG_RES
        }

        // Apply animations
        // Shake only applies during the cracking phase or transitioning to it
        val rotation = if (isCracking && !isHatched) shake.value else 0f
        
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "HatchBase Splash",
            modifier = Modifier
                .size(200.dp)
                .scale(scale.value)
                .rotate(rotation)
        )
    }
}
