@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.feature.incubation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.Incubation
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.data.models.Device
import com.example.hatchtracker.common.format.LocaleFormatService
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIncubationDialog(
    incubation: Incubation,
    devices: List<Device>,
    localeFormatService: LocaleFormatService,
    dateFormat: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String) -> Unit
) {
    val context = LocalContext.current
    val minimumEggs = incubation.hatchedCount + incubation.infertileCount + incubation.failedCount
    var startDate by remember { mutableStateOf(incubation.startDate) }
    var eggsCount by remember { mutableStateOf(incubation.eggsCount.toString()) }
    var notes by remember { mutableStateOf(incubation.notes ?: "") }
    var deviceId by remember { mutableStateOf(incubation.incubatorDeviceId) }

    val eggsError = remember(eggsCount) {
        eggsCount.toIntOrNull()?.let { it < minimumEggs } ?: true
    }

    var expandedDevice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(UiR.string.edit_incubation_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                val formattedStartDate = remember(startDate, dateFormat) {
                    localeFormatService.formatDate(startDate, dateFormat)
                }
                OutlinedTextField(
                    value = formattedStartDate,
                    onValueChange = { },
                    label = { Text(stringResource(UiR.string.start_date_format_hint)) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val current = runCatching {
                                java.time.LocalDate.parse(startDate)
                            }.getOrElse {
                                java.time.LocalDate.now()
                            }
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    startDate = java.time.LocalDate.of(y, m + 1, d).toString()
                                },
                                current.year,
                                current.monthValue - 1,
                                current.dayOfMonth
                            ).show()
                        }) {
                            Icon(Icons.Default.DateRange, stringResource(UiR.string.select_date_content_description))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = eggsCount,
                    onValueChange = { eggsCount = it },
                    label = { Text(stringResource(UiR.string.eggs_count_required_label)) },
                    isError = eggsError,
                    supportingText = if (eggsError) {
                        { Text(stringResource(UiR.string.min_eggs_required_format, minimumEggs)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDevice,
                    onExpandedChange = { expandedDevice = !expandedDevice },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val activeDevice = devices.find { it.id == deviceId }
                    OutlinedTextField(
                        value = activeDevice?.displayName ?: stringResource(UiR.string.none_manual),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(UiR.string.incubator_device_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDevice) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDevice,
                        onDismissRequest = { expandedDevice = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(UiR.string.none_manual)) },
                            onClick = {
                                deviceId = ""
                                expandedDevice = false
                            }
                        )
                        devices.filter { it.isActive && (it.type == com.example.hatchtracker.model.DeviceType.SETTER || it.type == com.example.hatchtracker.model.DeviceType.HATCHER) }.forEach { device ->
                            DropdownMenuItem(
                                text = { Text(device.displayName) },
                                onClick = {
                                    deviceId = device.id
                                    expandedDevice = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(UiR.string.notes_label)) },
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().testTag("EditIncubationNotesInput")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = eggsCount.toIntOrNull() ?: 0
                    if (!eggsError) {
                        onConfirm(startDate, count, notes, deviceId)
                    }
                },
                modifier = Modifier.testTag("EditIncubationSaveButton")
            ) {
                    Text(stringResource(UiR.string.save_changes_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("EditIncubationCancelButton")
            ) {
                    Text(stringResource(UiR.string.cancel))
            }
        },
        modifier = Modifier.testTag("EditIncubationDialog")
    )
}
