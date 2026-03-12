package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/**
 * ScreenScaffold guarantees consistent insets for top-level screens.
 *
 * Rules:
 * 1. Must NOT wrap another Scaffold internally.
 * 2. Proper hierarchy: Scaffold -> ScreenScaffold -> LazyColumn
 * 3. Handles safeDrawing, ime, navigationBars, and statusBars safely.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    useInsets: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current

    val insetsPadding = if (useInsets) {
        WindowInsets.safeDrawing
            .union(WindowInsets.ime)
            .union(WindowInsets.navigationBars)
            .union(WindowInsets.statusBars)
            .asPaddingValues()
    } else {
        PaddingValues(0.dp)
    }

    // Resolves padding values by taking maxOf to prevent double-padding when nested inside a Scaffold,
    // while still guaranteeing safety for IME or navigation items if the parent Scaffold missed them.
    val combinedPadding = PaddingValues(
        start = maxOf(insetsPadding.calculateStartPadding(layoutDirection), contentPadding.calculateStartPadding(layoutDirection)),
        top = maxOf(insetsPadding.calculateTopPadding(), contentPadding.calculateTopPadding()),
        end = maxOf(insetsPadding.calculateEndPadding(layoutDirection), contentPadding.calculateEndPadding(layoutDirection)),
        bottom = maxOf(insetsPadding.calculateBottomPadding(), contentPadding.calculateBottomPadding())
    )

    Box(modifier = modifier) {
        content(combinedPadding)
    }
}
