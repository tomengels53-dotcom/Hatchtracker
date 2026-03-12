package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val gradientColors = LocalAppGradientColors.current

    val fallbackTop = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    val topColor = if (gradientColors.screenTop != Color.Unspecified) gradientColors.screenTop else fallbackTop
    val bottomColor = if (gradientColors.screenBottom != Color.Unspecified) {
        gradientColors.screenBottom
    } else {
        MaterialTheme.colorScheme.background
    }
    val alphaCap = if (isDark) 0.06f else 0.08f

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            topColor.copy(alpha = alphaCap),
            bottomColor
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .background(brush = gradientBrush)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = AppSurfaceSpec.PaddingScreenTop,
                    start = AppSurfaceSpec.PaddingScreenHorizontal,
                    end = AppSurfaceSpec.PaddingScreenHorizontal,
                    bottom = 0.dp
                )
        ) {
            content()
        }
    }
}
