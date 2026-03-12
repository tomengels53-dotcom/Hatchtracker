package com.example.hatchtracker.feature.admin

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.data.models.GeneticProfile
import com.example.hatchtracker.feature.admin.BreedAdminViewModel
import com.example.hatchtracker.core.ui.R as UiR

import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BreedAdminScreen(
    onBack: () -> Unit,
    viewModel: BreedAdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingBreed by remember { mutableStateOf<BreedStandard?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.admin_breed_management_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        editingBreed = BreedStandard() 
                        isCreating = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(UiR.string.admin_add_breed_content_description))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search and Filters
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateSearch(it) },
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text(stringResource(UiR.string.admin_search_breeds_placeholder)) },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {}

            Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.officialOnly,
                    onClick = { viewModel.updateFilters(uiState.speciesFilter, !uiState.officialOnly) },
                    label = { Text(stringResource(UiR.string.admin_official_only)) }
                )
            }

            // Data Table Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(UiR.string.admin_table_name), modifier = Modifier.weight(1.5f), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                Text(stringResource(UiR.string.admin_table_species), modifier = Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                Text(stringResource(UiR.string.admin_table_official), modifier = Modifier.weight(0.7f), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                Text(stringResource(UiR.string.admin_table_actions), modifier = Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.filteredBreeds) { breed ->
                        BreedRow(
                            breed = breed,
                            onEdit = { 
                                editingBreed = breed 
                                isCreating = false
                            },
                            onDelete = { viewModel.softDelete(breed) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // Edit/Create Dialog
    if (editingBreed != null) {
        BreedEditDialog(
            breed = editingBreed!!,
            isCreating = isCreating,
            onDismiss = { editingBreed = null },
            onConfirm = { updated ->
                viewModel.initiateSave(updated, if (isCreating) null else editingBreed)
                editingBreed = null
            }
        )
    }

    // Save Confirmation Dialog
    if (uiState.showConfirmation && uiState.pendingBreed != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmation() },
            title = { Text(stringResource(UiR.string.admin_review_changes_title)) },
            text = {
                Column {
                    Text(stringResource(UiR.string.admin_commit_registry_confirm))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(UiR.string.admin_breed_label, uiState.pendingBreed!!.name), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                    Text(stringResource(UiR.string.admin_species_label, uiState.pendingBreed!!.species))
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmSave() }) {
                    if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text(stringResource(UiR.string.admin_commit_log))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmation() }) {
                    Text(stringResource(UiR.string.cancel))
                }
            }
        )
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text(stringResource(UiR.string.confirm_action)) } },
            title = { Text(stringResource(UiR.string.admin_error_title)) },
            text = { Text(uiState.error!!) }
        )
    }
}

@Composable
fun BreedRow(
    breed: BreedStandard,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(breed.name, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(breed.species, modifier = Modifier.weight(1f))
        Icon(
            if (breed.official) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (breed.official) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.weight(0.7f).size(20.dp)
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(UiR.string.admin_edit_content_description))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(UiR.string.admin_deprecate_content_description), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BreedEditDialog(
    breed: BreedStandard,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (BreedStandard) -> Unit
) {
    var state by remember { mutableStateOf(breed) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    if (isCreating) stringResource(UiR.string.admin_create_new_breed) else stringResource(UiR.string.admin_edit_breed_standard),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { state = state.copy(name = it) },
                            label = { Text(stringResource(UiR.string.admin_breed_name)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = state.species,
                            onValueChange = { state = state.copy(species = it) },
                            label = { Text(stringResource(UiR.string.admin_table_species)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.official, onCheckedChange = { state = state.copy(official = it) })
                            Text(stringResource(UiR.string.admin_official_breed_standard))
                        }
                    }
                    
                    item {
                        Text(stringResource(UiR.string.admin_weights_kg), style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.weightRoosterKg.toString(),
                                onValueChange = { state = state.copy(weightRoosterKg = it.toDoubleOrNull() ?: 0.0) },
                                label = { Text(stringResource(UiR.string.admin_rooster)) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state.weightHenKg.toString(),
                                onValueChange = { state = state.copy(weightHenKg = it.toDoubleOrNull() ?: 0.0) },
                                label = { Text(stringResource(UiR.string.admin_hen)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text(stringResource(UiR.string.admin_genetic_profile), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        GeneticSectionEditor(
                            label = stringResource(UiR.string.admin_known_genes),
                            items = state.geneticProfile.knownGenes,
                            onUpdate = { state = state.copy(geneticProfile = state.geneticProfile.copy(knownGenes = it)) }
                        )
                    }
                    
                    item {
                        GeneticSectionEditor(
                            label = stringResource(UiR.string.admin_fixed_traits),
                            items = state.geneticProfile.fixedTraits,
                            onUpdate = { state = state.copy(geneticProfile = state.geneticProfile.copy(fixedTraits = it)) }
                        )
                    }
                    
                    item {
                        Text(stringResource(UiR.string.admin_confidence_level))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConfidenceLevel.values().forEach { level ->
                                FilterChip(
                                    selected = state.geneticProfile.confidenceLevelEnum == level,
                                    onClick = { state = state.copy(geneticProfile = state.geneticProfile.copy(confidenceLevel = level.name)) },
                                    label = { Text(level.name) }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(state) }) { Text(stringResource(UiR.string.admin_save_standard)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GeneticSectionEditor(
    label: String,
    items: List<String>,
    onUpdate: (List<String>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        FlowRow(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEach { item ->
                InputChip(
                    selected = true,
                    onClick = { onUpdate(items - item) },
                    label = { Text(item) },
                    trailingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(UiR.string.admin_add_label_format, label)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { 
                    if (text.isNotBlank()) {
                        onUpdate(items + text.trim())
                        text = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        )
    }
}



