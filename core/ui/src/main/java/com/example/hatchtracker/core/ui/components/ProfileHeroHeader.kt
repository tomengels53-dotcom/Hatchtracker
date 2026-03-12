package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hatchtracker.core.ui.theme.AppSpacing
import com.example.hatchtracker.core.ui.util.ProfileImageState

/**
 * A hero header for detail screens that displays the profile picture or species fallback.
 * Support loading, editing actions, and premium overlays.
 */
@Composable
fun ProfileHeroHeader(
    state: ProfileImageState,
    onEditClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Main Image/Placeholder
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
                is ProfileImageState.LocalPhoto -> {
                    AsyncImage(
                        model = state.path,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                is ProfileImageState.SpeciesIcon -> {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = state.resId),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(0.6f) // Don't let icon touch edges
                            .padding(AppSpacing.spacing16),
                        contentScale = ContentScale.Fit
                    )
                }
                is ProfileImageState.GenericFallback -> {
                    Icon(
                        painter = painterResource(id = state.resId),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // Bottom Gradient Overlay for text readability
        if (title != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(AppSpacing.spacing16)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppSpacing.spacing16),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.spacing8)
        ) {
            if (onRemoveClick != null && state is ProfileImageState.LocalPhoto) {
                SmallFloatingActionButton(
                    onClick = onRemoveClick,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Photo")
                }
            }
            
            SmallFloatingActionButton(
                onClick = onEditClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                val icon = if (state is ProfileImageState.LocalPhoto) Icons.Default.Edit else Icons.Default.AddAPhoto
                Icon(icon, contentDescription = "Change Photo")
            }
        }
    }
}
