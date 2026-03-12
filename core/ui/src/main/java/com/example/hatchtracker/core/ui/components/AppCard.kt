package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.theme.AppSpacing

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    variant: AppCardVariant = AppCardVariant.STANDARD,
    shape: androidx.compose.ui.graphics.Shape? = null, // Allow override
    colors: androidx.compose.material3.CardColors? = null, // Allow override
    content: @Composable ColumnScope.() -> Unit
) {
    val finalShape = shape ?: when (variant) {
        AppCardVariant.HERO -> AppSurfaceSpec.ShapeHero
        AppCardVariant.STANDARD -> AppSurfaceSpec.ShapeScreenCard
        AppCardVariant.SUBTLE -> AppSurfaceSpec.ShapeListRow
    }

    val elevation = when (variant) {
        AppCardVariant.HERO -> AppSurfaceSpec.ElevationHero
        AppCardVariant.STANDARD -> AppSurfaceSpec.ElevationScreenCard
        AppCardVariant.SUBTLE -> AppSurfaceSpec.ElevationSubtle
    }

    val defaultContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
    val finalColors = colors ?: CardDefaults.cardColors(containerColor = defaultContainerColor)

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val border = if (isDark && variant != AppCardVariant.HERO) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    } else null

    val internalPadding = if (variant == AppCardVariant.HERO) AppSurfaceSpec.PaddingHeroInternal else AppSurfaceSpec.PaddingCardInternal

    val cardContentSpacing = when (variant) {
        AppCardVariant.HERO -> AppSpacing.spacing12
        AppCardVariant.STANDARD -> AppSpacing.spacing8
        AppCardVariant.SUBTLE -> AppSpacing.spacing8
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = finalShape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = border,
        colors = finalColors
    ) {
        Column(
            modifier = Modifier.padding(internalPadding),
            verticalArrangement = Arrangement.spacedBy(cardContentSpacing),
            content = content
        )
    }
}
