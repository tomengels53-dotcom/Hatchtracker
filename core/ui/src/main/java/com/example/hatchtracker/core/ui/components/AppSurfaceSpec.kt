package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AppSurfaceSpec {
    val RadiusHero = 28.dp
    val RadiusScreenCard = 24.dp
    val RadiusSmallTile = 20.dp
    val RadiusListRow = 18.dp
    val RadiusBottomSheet = 28.dp
    val RadiusInput = 18.dp
    val RadiusFloatingButton = 16.dp

    val ShapeHero = RoundedCornerShape(RadiusHero)
    val ShapeScreenCard = RoundedCornerShape(RadiusScreenCard)
    val ShapeSmallTile = RoundedCornerShape(RadiusSmallTile)
    val ShapeListRow = RoundedCornerShape(RadiusListRow)
    val ShapeBottomSheet = RoundedCornerShape(topStart = RadiusBottomSheet, topEnd = RadiusBottomSheet)
    val ShapeInput = RoundedCornerShape(RadiusInput)
    val ShapeFloatingButton = RoundedCornerShape(RadiusFloatingButton)
    val ShapeButton = ShapeInput

    val ElevationHero = 8.dp
    val ElevationScreenCard = 6.dp
    val ElevationSmallTile = 4.dp
    val ElevationSubtle = 2.dp
    val ElevationBottomSheet = 8.dp
    val ElevationInput = 2.dp
    val ElevationFloatingButton = 6.dp

    val PaddingScreenTop = 32.dp
    val PaddingSection = 24.dp
    val PaddingCardSpacing = 16.dp
    val PaddingCardInternal = 16.dp
    val PaddingHeroInternal = 28.dp
    val PaddingScreenHorizontal = 24.dp
}
