package com.example.hatchtracker.feature.flock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.catalog.DefaultCatalogData
import com.example.hatchtracker.core.ui.catalog.SpeciesOption
import com.example.hatchtracker.core.ui.R as UiR
import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.data.models.Flocklet


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddFlockletDialog(
    maxBreeds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Flocklet) -> Unit,
    onInfoClick: (String) -> Unit,
    getBreedsForSpecies: (String) -> List<BreedStandard>
) {
    val allSpecies = DefaultCatalogData.allSpecies
    var speciesExpanded by remember { mutableStateOf(false) }
    var selectedSpecies by remember { mutableStateOf<SpeciesOption?>(null) }

    // Breeds
    var breedExpanded by remember { mutableStateOf(false) }
    var selectedBreeds by remember { mutableStateOf(listOf<BreedStandard>()) }
    
    val availableBreeds = remember(selectedSpecies) {
        selectedSpecies?.name?.let { getBreedsForSpecies(it) } ?: emptyList()
    }
    val mixedBreedLabel = stringResource(UiR.string.mixed_breed_label)

    var count by remember { mutableStateOf("") }
    var ageDays by remember { mutableStateOf("0") }
    
    LaunchedEffect(selectedSpecies) {
        selectedBreeds = emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(UiR.string.add_manual_flocklet_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(UiR.string.add_manual_flocklet_description),
                    style = MaterialTheme.typography.bodySmall
                )
                
                ExposedDropdownMenuBox(
                    expanded = speciesExpanded,
                    onExpandedChange = { speciesExpanded = !speciesExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSpecies?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(UiR.string.species_label)) },
                        placeholder = { Text(stringResource(UiR.string.placeholder_select_species)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = speciesExpanded,
                        onDismissRequest = { speciesExpanded = false }
                    ) {
                        allSpecies.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    selectedSpecies = item
                                    speciesExpanded = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedBreeds.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedBreeds.forEach { breed ->
                            InputChip(
                                selected = true,
                                onClick = { selectedBreeds = selectedBreeds - breed },
                                label = { Text(breed.name, maxLines = 1) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(UiR.string.remove_action),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = breedExpanded,
                    onExpandedChange = { 
                        if (selectedBreeds.size < maxBreeds || breedExpanded) {
                            breedExpanded = !breedExpanded 
                        }
                    }
                ) {
                    val breedFieldEnabled = selectedSpecies != null && selectedBreeds.size < maxBreeds
                    val breedSupportingText: (@Composable () -> Unit)? = when {
                        selectedSpecies == null -> {
                            @Composable {
                                Text(
                                    stringResource(UiR.string.select_species_first_tip),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        selectedBreeds.size >= maxBreeds -> {
                            @Composable {
                                Text(
                                    stringResource(UiR.string.limit_reached_error, maxBreeds),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> null
                    }

                    OutlinedTextField(
                        value = if (selectedBreeds.isEmpty()) "" else stringResource(UiR.string.breeds_selected_count, selectedBreeds.size),
                        onValueChange = { },
                        readOnly = true,
                        enabled = breedFieldEnabled,
                        label = { Text(stringResource(UiR.string.add_breeds_label, maxBreeds)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = breedExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = breedFieldEnabled).fillMaxWidth(),
                        placeholder = { Text(stringResource(UiR.string.select_breeds_placeholder)) },
                        supportingText = breedSupportingText
                    )
                    ExposedDropdownMenu(
                        expanded = breedExpanded,
                        onDismissRequest = { breedExpanded = false }
                    ) {
                        when {
                            selectedSpecies == null -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(UiR.string.select_species_first_tip),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = { breedExpanded = false }
                                )
                            }
                            availableBreeds.isEmpty() -> {
                                DropdownMenuItem(
                                    text = { Text(stringResource(UiR.string.no_standard_breeds_found)) },
                                    onClick = { breedExpanded = false }
                                )
                            }
                            else -> {
                                availableBreeds.forEach { breed ->
                                    val isSelected = selectedBreeds.any { it.id == breed.id }
                                    DropdownMenuItem(
                                        text = { 
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = null
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(breed.name, modifier = Modifier.weight(1f))
                                                IconButton(
                                                    onClick = { onInfoClick(breed.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Info,
                                                        contentDescription = stringResource(UiR.string.details_action),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            if (isSelected) {
                                                selectedBreeds = selectedBreeds.filter { it.id != breed.id }
                                            } else if (selectedBreeds.size < maxBreeds) {
                                                selectedBreeds = selectedBreeds + breed
                                            }
                                            if (selectedBreeds.size >= maxBreeds) breedExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = count,
                    onValueChange = { if (it.all { c -> c.isDigit() }) count = it },
                    label = { Text(stringResource(UiR.string.chick_count_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ageDays,
                    onValueChange = { if (it.all { c -> c.isDigit() }) ageDays = it },
                    label = { Text(stringResource(UiR.string.age_days_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text(stringResource(UiR.string.just_hatched_tip)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val selectedSpeciesName = selectedSpecies?.name ?: ""
            Button(
                onClick = {
                    if (selectedSpeciesName.isNotBlank() && count.isNotBlank()) {
                        val hatchDate = System.currentTimeMillis() - (ageDays.toLongOrNull() ?: 0) * 86400000L
                        val newFlocklet = Flocklet(
                            id = 0,
                            hatchId = null,
                            species = selectedSpeciesName,
                            breeds = selectedBreeds.map { it.name }.ifEmpty { listOf(mixedBreedLabel) },
                            hatchDate = hatchDate,
                            chickCount = count.toInt(),
                            currentTemp = 0.0,
                            targetTemp = 0.0,
                            ageInDays = ageDays.toIntOrNull() ?: 0,
                            healthStatus = "Healthy",
                            notes = null,
                            syncId = java.util.UUID.randomUUID().toString(),
                            lastUpdated = System.currentTimeMillis()
                        )
                        onConfirm(newFlocklet)
                    }
                },
                enabled = selectedSpeciesName.isNotBlank() && count.isNotBlank()
            ) {
                Text(stringResource(UiR.string.add_manual_flocklet_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(UiR.string.cancel))
            }
        }
    )
}
