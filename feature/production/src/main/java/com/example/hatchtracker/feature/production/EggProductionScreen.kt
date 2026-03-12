@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.feature.production

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.R as UiR
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EggProductionScreen(
    onBackClick: () -> Unit,
    viewModel: EggProductionViewModel = hiltViewModel()
) {
    val flocks by viewModel.activeFlocks.collectAsState()
    val breedLines by viewModel.breedLines.collectAsState()
    val selectedFlockId by viewModel.selectedFlockId.collectAsState()
    val selectedLineId by viewModel.selectedLineId.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    val totalEggs by viewModel.totalEggs.collectAsState()
    val crackedEggs by viewModel.crackedEggs.collectAsState()
    val setEggs by viewModel.setEggs.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    // Snack bar state
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(viewModel.saveSuccess) {
        viewModel.saveSuccess.collect {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.log_egg_production_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flock Selection
            var flockExpanded by remember { mutableStateOf(false) }
            val selectedFlockName = flocks.find { it.syncId == selectedFlockId }?.name ?: "Select Flock"
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { flockExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedFlockName)
                }
                DropdownMenu(
                    expanded = flockExpanded,
                    onDismissRequest = { flockExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    flocks.forEach { flock ->
                        DropdownMenuItem(
                            text = { Text(flock.name) },
                            onClick = {
                                viewModel.selectFlock(flock.syncId)
                                flockExpanded = false
                            }
                        )
                    }
                }
            }

            // Breed Line Selection (Only if lines exist)
            if (breedLines.isNotEmpty()) {
                var lineExpanded by remember { mutableStateOf(false) }
                val selectedLineName = breedLines.find { it.cloudId == selectedLineId }?.label ?: "All / Mixed"
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { lineExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(UiR.string.label_line_prefix, selectedLineName))
                    }
                    DropdownMenu(
                        expanded = lineExpanded,
                        onDismissRequest = { lineExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(UiR.string.label_all_mixed)) },
                            onClick = {
                                viewModel.selectLine(null)
                                lineExpanded = false
                            }
                        )
                        breedLines.forEach { line ->
                            DropdownMenuItem(
                                text = { Text(line.label) },
                                onClick = {
                                    viewModel.selectLine(line.cloudId)
                                    lineExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Date Selection (Simple Text for now, or DatePicker)
            // Using a simple read-only text field that triggers a hypothetical date picker or just showing today
            OutlinedTextField(
                value = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(UiR.string.label_date)) },
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            // NOTE: production-grade would use DatePickerDialog here.

            HorizontalDivider()

            // Inputs
            OutlinedTextField(
                value = totalEggs,
                onValueChange = { viewModel.updateTotal(it) },
                label = { Text(stringResource(UiR.string.label_total_eggs_collected)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = crackedEggs,
                    onValueChange = { viewModel.updateCracked(it) },
                    label = { Text(stringResource(UiR.string.label_cracked_damaged)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = setEggs,
                    onValueChange = { viewModel.updateSet(it) },
                    label = { Text(stringResource(UiR.string.label_set_for_incubation)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text(stringResource(UiR.string.notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Button(
                onClick = { viewModel.saveProduction() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && selectedFlockId != null
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(UiR.string.action_save_log))
                }
            }
        }
    }
}
