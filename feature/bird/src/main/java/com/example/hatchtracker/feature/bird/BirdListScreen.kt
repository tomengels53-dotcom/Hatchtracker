package com.example.hatchtracker.feature.bird

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Sex
import com.example.hatchtracker.core.ui.composeutil.premiumClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.feature.bird.BirdListViewModel
import com.example.hatchtracker.core.ui.components.ProfileCompactRowItem
import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.core.ui.R as UiR

@Composable
fun BirdListScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAddBirdClick: () -> Unit = {},
    onBirdClick: (Long) -> Unit = {},
    viewModel: BirdListViewModel = hiltViewModel()
) {
    val birdList by viewModel.allBirds.collectAsState()
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackClick) {
                    Text(stringResource(UiR.string.back_action))
                }
                Text(
                    text = stringResource(UiR.string.bird_list_title),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                )
                Text(
                    text = stringResource(UiR.string.bird_count_format, birdList.size),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // List of birds
        if (birdList.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(UiR.string.no_birds_yet),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(UiR.string.add_first_bird_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(birdList, key = { it.id }) { bird ->
                    ProfileCompactRowItem(
                        title = UiText.DynamicString(bird.breed),
                        subtitle = UiText.DynamicString("${bird.species.name} • ${bird.sex}"),
                        imagePath = bird.imagePath,
                        species = bird.species,
                        statusText = UiText.DynamicString(bird.hatchDate),
                        onClick = { onBirdClick(bird.id) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                }
            }
        }
        
        // Add Bird button
        Button(
            onClick = onAddBirdClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(UiR.string.add_new_bird_action))
        }
    }
}
