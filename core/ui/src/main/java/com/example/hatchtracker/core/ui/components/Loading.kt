package com.example.hatchtracker.core.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Premium Shimmer Effect (Gradient Translation)
 * Very low contrast, slow speed (1000ms)
 */
fun Modifier.premiumShimmer(): Modifier = composed {
    val shimmerColors = if (isSystemInDarkTheme()) {
        listOf(
            Color.White.copy(alpha = 0.015f),
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.015f),
        )
    } else {
        listOf(
            Color.Black.copy(alpha = 0.03f),
            Color.Black.copy(alpha = 0.07f),
            Color.Black.copy(alpha = 0.03f),
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim + 400f, translateAnim + 400f)
    )

    this.background(brush = brush)
}

/**
 * Skeleton container mimicking a Card structure exactly.
 * Ensures consistent final layout spacing.
 */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(20.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .premiumShimmer()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .premiumShimmer()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .premiumShimmer()
            )
        }
    }
}

/**
 * Skeleton container mimicking a list row exactly.
 */
@Composable
fun SkeletonListRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon circular placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .premiumShimmer()
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .premiumShimmer()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .premiumShimmer()
            )
        }
    }
}
