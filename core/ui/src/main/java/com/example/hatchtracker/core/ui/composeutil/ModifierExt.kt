package com.example.hatchtracker.core.ui.composeutil

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * Applies a premium "instrument-grade" click interaction.
 *
 * It uses a subtle scale down effect to 0.98f when pressed.
 * It enforces a minimum interactive component size of 48dp globally.
 *
 * Constraints:
 * 1. Must be used ONLY on leaf tappables (Card surfaces, List rows, Selectable chips).
 * 2. Do NOT apply to entire screens or scrollable root containers (e.g., LazyColumn).
 * 3. Do NOT apply to parents that contain nested clickable children.
 * 4. Do NOT apply to containers with TextFields.
 */
fun Modifier.premiumClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    indication: Indication? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(
            durationMillis = if (isPressed) 100 else 140,
            easing = FastOutSlowInEasing
        ),
        label = "PremiumClickScale"
    )

    this
        .minimumInteractiveComponentSize()
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = indication ?: ripple(),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
}

/**
 * Applies a premium "instrument-grade" combined click interaction (supports long clicks).
 *
 * It uses a subtle scale down effect to 0.98f when pressed.
 * It enforces a minimum interactive component size of 48dp globally.
 *
 * Constraints: Same as [premiumClickable].
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.premiumCombinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(
            durationMillis = if (isPressed) 100 else 140,
            easing = FastOutSlowInEasing
        ),
        label = "PremiumCombinedClickScale"
    )

    this
        .minimumInteractiveComponentSize()
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = onClick
        )
}
