package com.example.hatchtracker.feature.breeding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BreedSearchInsightsScreen(
    onBack: () -> Unit,
    onBreedClick: (String) -> Unit,
    viewModel: BreedSearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val breeds by viewModel.filteredBreeds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.breeding_home_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(UiR.string.breeding_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (breeds.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(UiR.string.no_breeds_match_search),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                items(breeds) { breed ->
                    BreedSearchRow(
                        breed = breed,
                        onClick = {
                            viewModel.recordSelection(breed.id)
                            onBreedClick(breed.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BreedSearchRow(
    breed: BreedStandard,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(breed.name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(UiR.string.breeding_species_origin_format, breed.species, breed.origin))
                val insights = listOfNotNull(
                    breed.eggColor.takeIf { it.isNotBlank() }?.let { stringResource(UiR.string.breeding_egg_label_format, it) },
                    breed.category?.takeIf { it.isNotBlank() }?.let { stringResource(UiR.string.breeding_type_label_format, it) },
                    breed.geneticProfile.fixedTraits.firstOrNull()?.let { stringResource(UiR.string.breeding_trait_label_format, it) }
                )
                if (insights.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Text(insights.joinToString("  |  "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

