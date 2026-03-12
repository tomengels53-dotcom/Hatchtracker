package com.example.hatchtracker.feature.breeding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.navigation.BreedSelectionResult
import com.example.hatchtracker.core.ui.catalog.DefaultCatalogData
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant
import com.example.hatchtracker.core.ui.composeutil.premiumClickable
import com.example.hatchtracker.core.ui.theme.AppSpacing
import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedSelectionScreen(
    speciesId: String,
    onBreedSelected: (BreedSelectionResult) -> Unit,
    onBackClick: () -> Unit,
    viewModel: BreedSelectionViewModel = hiltViewModel()
) {
    val breeds by viewModel.breeds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var infoBreed by remember { mutableStateOf<BreedStandard?>(null) }

    val speciesName = remember(speciesId) {
        DefaultCatalogData.allSpecies.find { it.id == speciesId }?.name ?: speciesId
    }
    val mixedLabel = stringResource(UiR.string.mixed_unknown_breed)

    LaunchedEffect(speciesName) {
        viewModel.loadBreeds(speciesName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.select_breed_content_description)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.spacing16),
                placeholder = { Text(stringResource(UiR.string.admin_search_breeds_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            AppCard(
                variant = AppCardVariant.SUBTLE,
                modifier = Modifier
                    .padding(horizontal = AppSpacing.spacing16)
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = AppSpacing.spacing16)
                ) {
                    if (breeds.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppSpacing.spacing32),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (searchQuery.isEmpty()) stringResource(UiR.string.no_breeds_for_species)
                                    else stringResource(UiR.string.no_breeds_match_search),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(breeds) { breed ->
                        ListItem(
                            headlineContent = { Text(breed.name) },
                            supportingContent = { Text(breed.origin) },
                            trailingContent = {
                                IconButton(onClick = { infoBreed = breed }) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = stringResource(UiR.string.breed_info_content_description),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.premiumClickable {
                                onBreedSelected(
                                    BreedSelectionResult(
                                        speciesId = speciesId,
                                        speciesName = speciesName,
                                        breedId = breed.id.toString(), // breed.id is Long? let me check
                                        breedName = breed.name
                                    )
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = AppSpacing.spacing16))
                    }

                    // Option for Mixed/Custom
                    item {
                        ListItem(
                            headlineContent = { Text(mixedLabel) },
                            supportingContent = { Text(stringResource(UiR.string.select_mixed_breed_help)) },
                            modifier = Modifier.premiumClickable {
                                onBreedSelected(
                                    BreedSelectionResult(
                                        speciesId = speciesId,
                                        speciesName = speciesName,
                                        breedId = "mixed",
                                        breedName = mixedLabel
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    BreedInfoDialog(breed = infoBreed, onDismiss = { infoBreed = null })
}

@Composable
private fun BreedInfoDialog(
    breed: BreedStandard?,
    onDismiss: () -> Unit
) {
    if (breed == null) return

    val detailEntries = buildList {
        addDetailEntry(stringResource(UiR.string.origin_label), breed.origin)
        addDetailEntry(stringResource(UiR.string.species_label), breed.species)
        addDetailEntry(stringResource(UiR.string.egg_color_label), breed.eggColor)
        addDetailEntry(stringResource(UiR.string.accepted_colors_label), breed.acceptedColors.joinToString())
        if (breed.weightRoosterKg > 0 || breed.weightHenKg > 0) {
            val weights = listOfNotNull(
                breed.weightRoosterKg.takeIf { it > 0 }?.let { stringResource(UiR.string.rooster_weight_format, it) },
                breed.weightHenKg.takeIf { it > 0 }?.let { stringResource(UiR.string.hen_weight_format, it) }
            ).joinToString(" / ")
            if (weights.isNotBlank()) addDetailEntry(stringResource(UiR.string.weight_label), weights)
        }
        addDetailEntry(stringResource(UiR.string.category_label), breed.category)
        addDetailEntry(stringResource(UiR.string.comb_label), breed.combType)
        addDetailEntry(stringResource(UiR.string.feather_label), breed.featherType)
        addDetailEntry(stringResource(UiR.string.skin_color_label), breed.skinColor)
        addDetailEntry(stringResource(UiR.string.earlobe_color_label), breed.earlobeColor)
        addDetailEntry(stringResource(UiR.string.recognized_by_label), breed.recognizedBy.joinToString())
        if (breed.isTrueBantam) addDetailEntry(stringResource(UiR.string.true_bantam_label), stringResource(UiR.string.yes))
        if (breed.official) addDetailEntry(stringResource(UiR.string.official_standard_label), stringResource(UiR.string.yes))
        addDetailEntry(stringResource(UiR.string.tags_label), breed.tags.joinToString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(breed.name.ifBlank { stringResource(UiR.string.breed_info_title_fallback) }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.spacing8)) {
                if (detailEntries.isEmpty()) {
                    Text(
                        stringResource(UiR.string.no_data_available_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    detailEntries.forEach { (label, value) ->
                        Column {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                            Text(value, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(UiR.string.close))
            }
        }
    )
}

private fun MutableList<Pair<String, String>>.addDetailEntry(label: String, value: String?) {
    if (!value.isNullOrBlank()) add(label to value)
}
