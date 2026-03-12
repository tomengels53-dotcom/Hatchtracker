package com.example.hatchtracker.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ThemeToggle(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val toggleWidth = 200.dp
    val toggleHeight = 56.dp
    val thumbSize = 48.dp
    val thumbPadding = 4.dp

    val offset by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "toggle_offset"
    )

    Box(
        modifier = modifier
            .size(width = toggleWidth, height = toggleHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (isDark) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onToggle(!isDark)
            }
            .padding(thumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        // Labels
        Row(
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Dark Mode Label (Visible when Dark, Thumb is on Right)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isDark) {
                    Text(
                        text = "Dark",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right Side: Light Mode Label (Visible when Light, Thumb is on Left)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (!isDark) {
                    Text(
                        text = "Light",
                        color = Color.Black, // Or onSurface
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Thumb
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((toggleWidth - thumbSize - thumbPadding * 2).toPx() * offset).roundToInt(),
                        y = 0
                    )
                }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
