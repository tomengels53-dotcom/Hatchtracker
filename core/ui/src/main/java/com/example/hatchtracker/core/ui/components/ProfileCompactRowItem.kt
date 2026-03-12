package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hatchtracker.core.ui.theme.AppSpacing
import com.example.hatchtracker.core.ui.util.ProfileImageState
import com.example.hatchtracker.core.ui.util.rememberProfileImageState
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.UiText

@Composable
fun ProfileCompactRowItem(
    title: UiText,
    subtitle: UiText,
    imagePath: String?,
    species: Species,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    statusText: UiText? = null,
    genderIcon: Int? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    iconColor: Color = MaterialTheme.colorScheme.primary, // Fallback if leadingContent fails?
    trailingContent: (@Composable () -> Unit)? = null
) {
    val profileState = rememberProfileImageState(imagePath, species, genderIcon)

    CompactRowItem(
        title = title,
        subtitle = subtitle,
        statusText = statusText,
        iconColor = iconColor,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        leadingContent = {
            ProfileImageAvatar(
                state = profileState,
                size = 40.dp // Hardened size for scannability
            )
        },
        trailingContent = trailingContent
    )
}

@Composable
fun ProfileImageAvatar(
    state: ProfileImageState,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
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
                Image(
                    painter = painterResource(id = state.resId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }
            is ProfileImageState.GenericFallback -> {
                Icon(
                    painter = painterResource(id = state.resId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.spacing8),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Actually, looking at CompactRowItem.kt, it has a fixed Box(iconColor) on the left.
// I should probably EXTEND CompactRowItem or create a similar one that allows a custom leading component.
// Let's check CompactRowItem again.
