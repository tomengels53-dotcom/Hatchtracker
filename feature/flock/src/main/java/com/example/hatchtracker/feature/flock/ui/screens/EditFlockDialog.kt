package com.example.hatchtracker.feature.flock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFlockDialog(
    flock: Flock,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(flock.name) }
    var notes by remember { mutableStateOf(flock.notes ?: "") }
    var purpose by remember { mutableStateOf(flock.purpose) }
    var active by remember { mutableStateOf(flock.active) }
    var breeds by remember { mutableStateOf(flock.breeds.joinToString(", ")) }
    
    var nameError by remember { mutableStateOf(false) }
    var expandedPurpose by remember { mutableStateOf(false) }
    val purposeOptions = listOf(
        "eggs" to UiR.string.flock_purpose_eggs,
        "breeding" to UiR.string.flock_purpose_breeding,
        "meat" to UiR.string.flock_purpose_meat,
        "exhibition" to UiR.string.flock_purpose_exhibition,
        "mixed" to UiR.string.flock_purpose_mixed
    )
    val mixedBreedLabel = stringResource(UiR.string.mixed_breed_label)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(UiR.string.edit_flock_action)) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = it.isBlank()
                        },
                        label = { Text(stringResource(UiR.string.flock_name_required_label)) },
                        isError = nameError,
                        supportingText = if (nameError) { { Text(stringResource(UiR.string.flock_name_empty_error)) } } else null,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("EditFlockNameInput")
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedPurpose,
                        onExpandedChange = { expandedPurpose = !expandedPurpose },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = purposeOptions.firstOrNull { it.first == purpose }?.let { stringResource(it.second) } ?: purpose.replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(UiR.string.flock_purpose_required_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPurpose) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPurpose,
                            onDismissRequest = { expandedPurpose = false }
                        ) {
                            purposeOptions.forEach { (key, labelRes) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(labelRes)) },
                                    onClick = {
                                        purpose = key
                                        expandedPurpose = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = breeds,
                        onValueChange = { breeds = it },
                        label = { Text(stringResource(UiR.string.breeds_comma_separated_label)) },
                        placeholder = { Text(stringResource(UiR.string.breeds_example_placeholder)) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth().testTag("EditFlockBreedsInput")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(UiR.string.active_flock_label), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(UiR.string.inactive_flocks_archived_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = active,
                            onCheckedChange = { active = it },
                            modifier = Modifier.testTag("EditFlockActiveSwitch")
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(UiR.string.notes_optional_label)) },
                        minLines = 3,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth().testTag("EditFlockNotesInput")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        val breedList = breeds.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .ifEmpty { listOf(mixedBreedLabel) }
                        onConfirm(name, notes, purpose, active, breedList)
                    }
                },
                modifier = Modifier.testTag("EditFlockSaveButton")
            ) {
                Text(stringResource(UiR.string.save_changes_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("EditFlockCancelButton")
            ) {
                Text(stringResource(UiR.string.cancel))
            }
        },
        modifier = Modifier.testTag("EditFlockDialog")
    )
}
