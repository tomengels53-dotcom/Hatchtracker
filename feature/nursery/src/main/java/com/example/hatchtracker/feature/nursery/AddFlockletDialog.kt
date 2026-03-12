package com.example.hatchtracker.feature.nursery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Lock
import com.example.hatchtracker.data.models.Species
import com.example.hatchtracker.data.repository.DataRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddFlockletDialog(
    state: NurseryViewModel.ManualFlockletState,
    maxBreeds: Int,
    gatedSpecies: List<com.example.hatchtracker.domain.util.SpeciesUiRow>,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpdateState: (com.example.hatchtracker.data.models.Species?, String, String) -> Unit, // species, count, age
    onRemoveBreed: (String) -> Unit,
    onNavigateToBreedSelection: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var speciesExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_manual_flocklet_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.add_manual_flocklet_description),
                    style = MaterialTheme.typography.bodySmall
                )
                
                // Species Dropdown
                ExposedDropdownMenuBox(
                    expanded = speciesExpanded,
                    onExpandedChange = { speciesExpanded = !speciesExpanded }
                ) {
                    OutlinedTextField(
                        value = state.species?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.species_label)) },
                        placeholder = { Text(stringResource(R.string.placeholder_select_species)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                            .clickable { speciesExpanded = true }
                    )
                    ExposedDropdownMenu(
                        expanded = speciesExpanded,
                        onDismissRequest = { speciesExpanded = false }
                    ) {
                        gatedSpecies.forEach { speciesRow ->
                            val iconRes = com.example.hatchtracker.core.ui.catalog.DefaultCatalogData.allSpecies
                                .find { it.name.equals(speciesRow.displayName, ignoreCase = true) }
                                ?.imageResId

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Icon if available
                                        if (iconRes != null) {
                                            androidx.compose.foundation.Image(
                                                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp).padding(end = 8.dp)
                                            )
                                        }

                                        // Species name
                                        Text(
                                            text = speciesRow.displayName,
                                            modifier = Modifier.weight(1f),
                                            color = if (speciesRow.isLocked) 
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            else 
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        // Lock icon for locked species
                                        if (speciesRow.isLocked) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = stringResource(R.string.locked_content_description),
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    if (speciesRow.isLocked) {
                                        // Show snackbar for locked species
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = com.example.hatchtracker.domain.util.SpeciesGatingHelper.getUpgradeMessage(speciesRow.displayName),
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        speciesExpanded = false
                                    } else {
                                        val selectedSpecies = DataRepository.allSpecies.find { 
                                            it.name.equals(speciesRow.displayName, ignoreCase = true) 
                                        }
                                        onUpdateState(selectedSpecies, state.chickCount, state.ageDays)
                                        speciesExpanded = false
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Breed Selection (Unified Flow)
                if (state.species != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                         Text(stringResource(R.string.add_breeds_label, maxBreeds), style = MaterialTheme.typography.titleSmall)
                         
                         if (state.breedDetails.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                state.breedDetails.forEach { breed ->
                                    InputChip(
                                        selected = true,
                                        onClick = { onRemoveBreed(breed.breedId) },
                                        label = { Text(breed.breedName) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = stringResource(R.string.remove_action),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        
                        if (state.breedDetails.size < maxBreeds) {
                            OutlinedButton(
                                onClick = { 
                                    val speciesRow = gatedSpecies.find { it.displayName.equals(state.species?.name, ignoreCase = true) }
                                    if (speciesRow != null && speciesRow.isLocked) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = com.example.hatchtracker.domain.util.SpeciesGatingHelper.getUpgradeMessage(state.species?.name ?: "Species"),
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    } else {
                                        onNavigateToBreedSelection(state.species?.id ?: "mixed") 
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.add_breed_action))
                            }
                        } else {
                            Text(
                                stringResource(R.string.limit_reached_error, maxBreeds),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.select_species_first_tip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = state.chickCount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onUpdateState(state.species, it, state.ageDays) },
                    label = { Text(stringResource(R.string.chick_count_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.ageDays,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onUpdateState(state.species, state.chickCount, it) },
                    label = { Text(stringResource(R.string.age_days_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text(stringResource(R.string.just_hatched_tip)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = state.species != null && state.chickCount.isNotBlank()
            ) {
                Text(stringResource(R.string.add_manual_flocklet_title))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
