package com.example.hatchtracker.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.model.EquipmentMaintenanceLog
import com.example.hatchtracker.model.MaintenanceLogType
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun MaintenanceLogDialog(
    deviceId: String,
    deviceName: String,
    logs: List<EquipmentMaintenanceLog>,
    onAddLog: (EquipmentMaintenanceLog) -> Unit,
    onDeleteLog: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.maintenance_log_dialog_title, deviceName)) },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.maintenance_log_close_desc))
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.maintenance_log_add_button))
                        }
                    }
                ) { padding ->
                    if (logs.isEmpty()) {
                        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.maintenance_log_empty_msg), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(padding).fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(logs) { log ->
                                MaintenanceLogItem(log = log, onDelete = { onDeleteLog(log.id) })
                            }
                        }
                    }
                }
            }
        }
    )

    if (showAddDialog) {
        AddMaintenanceLogDialog(
            deviceId = deviceId,
            onDismiss = { showAddDialog = false },
            onConfirm = { log ->
                onAddLog(log)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MaintenanceLogItem(log: EquipmentMaintenanceLog, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.maintenance_log_item_title,
                        log.type.name,
                        DateFormat.getDateInstance().format(Date(log.date))
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (log.description.isNotBlank()) {
                    Text(text = log.description, style = MaterialTheme.typography.bodySmall)
                }
                val logCost = log.cost
                if (logCost != null) {
                    Text(
                        text = stringResource(R.string.maintenance_log_cost_format, logCost),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.maintenance_log_delete_desc), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddMaintenanceLogDialog(
    deviceId: String,
    onDismiss: () -> Unit,
    onConfirm: (EquipmentMaintenanceLog) -> Unit
) {
    var type by remember { mutableStateOf(MaintenanceLogType.CLEANING) }
    var description by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.maintenance_log_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.maintenance_log_type_label), style = MaterialTheme.typography.labelSmall)
                // Simple Radio button list for type
                MaintenanceLogType.entries.forEach { logType ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { type = logType }
                    ) {
                        RadioButton(selected = type == logType, onClick = { type = logType })
                        Text(logType.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.maintenance_log_description_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = costInput,
                    onValueChange = { costInput = it },
                    label = { Text(stringResource(R.string.maintenance_log_cost_label)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(EquipmentMaintenanceLog(
                    equipmentId = deviceId,
                    type = type,
                    description = description,
                    cost = costInput.toDoubleOrNull()
                ))
            }) {
                Text(stringResource(R.string.maintenance_log_add_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
