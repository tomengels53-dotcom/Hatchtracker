package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.R

@Composable
fun HatchyAccessibilityIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.hatchy_1),
            contentDescription = "Ask Hatchy",
            modifier = Modifier.size(32.dp)
        )
    }
}
