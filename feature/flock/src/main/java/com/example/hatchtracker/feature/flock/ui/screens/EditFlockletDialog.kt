package com.example.hatchtracker.feature.flock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.Flocklet
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFlockletDialog(
    flocklet: Flocklet,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var healthStatus by remember { mutableStateOf(flocklet.healthStatus) }
    var notes by remember { mutableStateOf(flocklet.notes ?: "") }
    
    val healthOptions = listOf("Healthy", "Issues", "Critical")
    val healthLabelMap = mapOf(
        "Healthy" to stringResource(UiR.string.health_status_healthy),
        "Issues" to stringResource(UiR.string.health_status_issues),
        "Critical" to stringResource(UiR.string.health_status_critical)
    )
    var healthExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(UiR.string.update_flocklet_status_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = healthExpanded,
                    onExpandedChange = { healthExpanded = !healthExpanded }
                ) {
                    OutlinedTextField(
                        value = healthStatus,
                        onValueChange = {},
                        readOnly = true,
                        visualTransformation = androidx.compose.ui.text.input.VisualTransformation { text ->
                            val replacement = healthLabelMap[text.text] ?: text.text
                            androidx.compose.ui.text.input.TransformedText(
                                androidx.compose.ui.text.AnnotatedString(replacement),
                                androidx.compose.ui.text.input.OffsetMapping.Identity
                            )
                        },
                        label = { Text(stringResource(UiR.string.health_status_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = healthExpanded) },
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = healthExpanded,
                        onDismissRequest = { healthExpanded = false }
                    ) {
                        healthOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(healthLabelMap[status] ?: status) },
                                onClick = {
                                    healthStatus = status
                                    healthExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(UiR.string.notes_label)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(healthStatus, notes)
                }
            ) {
                Text(stringResource(UiR.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(UiR.string.cancel))
            }
        }
    )
}

