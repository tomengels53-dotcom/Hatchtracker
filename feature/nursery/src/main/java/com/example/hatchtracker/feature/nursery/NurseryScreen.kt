// LAYOUT COMPOSITION CONTRACT
// This screen conforms strictly to the pre-polish layout baseline.
// Do NOT alter container structure or component layout without explicit design approval.
package com.example.hatchtracker.feature.nursery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import kotlinx.coroutines.launch
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R as UiR
import com.example.hatchtracker.feature.nursery.R as NurR
import com.example.hatchtracker.core.domain.models.ActionItem
import com.example.hatchtracker.core.domain.models.StandardActionModel
import com.example.hatchtracker.core.ui.components.StandardActionSheet
import com.example.hatchtracker.core.ui.components.StandardFilterBar
import com.example.hatchtracker.core.ui.components.CompactRowItem
import com.example.hatchtracker.core.ui.components.UrgencySectionHeader
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.hatchtracker.feature.nursery.models.NurseryRowModel
import com.example.hatchtracker.core.domain.models.ListSection
import com.example.hatchtracker.model.UiText


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurseryScreen(
    onBackClick: () -> Unit,
    onAddFinancialEntry: (String, String, Boolean) -> Unit,
    onSellFlocklet: (String, String) -> Unit,
    onNavigateToBreedDetail: (String) -> Unit = {},
    onNavigateToAddFlock: (String, List<String>, Long) -> Unit = { _, _, _ -> },
    onNavigateToBreedSelection: (String) -> Unit = {},
    onNavigateToIncubation: () -> Unit = {},
    onNavigateToEquipment: () -> Unit = {},
    onDeepdiveClick: (String, String) -> Unit = { _, _ -> },
    viewModel: com.example.hatchtracker.feature.nursery.NurseryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val caps by viewModel.currentCapabilities.collectAsState()
    val availableFlocks by viewModel.flocks.collectAsState()
    val manualFlockletState by viewModel.manualFlockletState.collectAsState()
    val hasCompletedIncubations by viewModel.hasCompletedIncubations.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isDeveloper by viewModel.isDeveloper.collectAsState()
    val canAccessFinance = FeatureAccessPolicy
        .canAccess(FeatureKey.FINANCE, caps.tier, isAdmin || isDeveloper)
        .allowed

    val canDeepdive = caps.tier == com.example.hatchtracker.data.models.SubscriptionTier.EXPERT ||
                      caps.tier == com.example.hatchtracker.data.models.SubscriptionTier.PRO ||
                      isAdmin || isDeveloper
    
    // State for Add Dialog
    var showAddDialog by remember { mutableStateOf(false) }
    
    // State for Action Sheet
    var selectedRowForAction by remember { mutableStateOf<NurseryRowModel?>(null) }
    
    // Snackbar for gating messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Gated species metadata
    val gatedSpecies = remember(caps.tier, isAdmin, isDeveloper) {
        viewModel.getSpeciesUiRows()
    }

    // State for Edit Dialog
    var showEditDialog by remember { mutableStateOf<com.example.hatchtracker.data.models.Flocklet?>(null) }
    var showDeleteDialog by remember { mutableStateOf<com.example.hatchtracker.data.models.Flocklet?>(null) }
    var showSelectFlockDialog by remember { mutableStateOf<com.example.hatchtracker.data.models.Flocklet?>(null) }
    var showRecordLossDialog by remember { mutableStateOf<com.example.hatchtracker.data.models.Flocklet?>(null) }

    if (showAddDialog) {
        AddFlockletDialog(
            state = manualFlockletState,
            maxBreeds = caps.maxBreedsPerBatch,
            gatedSpecies = gatedSpecies,
            snackbarHostState = snackbarHostState,
            onDismiss = { showAddDialog = false },
            onConfirm = {
                // Save-time validation for species gating
                val selectedSpecies = manualFlockletState.species?.name ?: ""
                val speciesRow = gatedSpecies.find { it.displayName.equals(selectedSpecies, ignoreCase = true) }
                
                if (speciesRow != null && speciesRow.isLocked) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = com.example.hatchtracker.domain.util.SpeciesGatingHelper.getUpgradeMessage(selectedSpecies),
                            duration = SnackbarDuration.Long
                        )
                    }
                } else {
                    viewModel.submitManualFlocklet()
                    showAddDialog = false
                }
            },
            onUpdateState = { species, count, age ->
                viewModel.updateManualFlockletState(species, count, age)
            },
            onRemoveBreed = { breedId ->
                viewModel.removeBreed(breedId)
            },
            onNavigateToBreedSelection = { speciesId ->
                onNavigateToBreedSelection(speciesId)
            }
        )
    }

    // Edit Dialog
    showEditDialog?.let { flockletToEdit ->
        EditFlockletDialog(
            flocklet = flockletToEdit,
            onDismiss = { showEditDialog = null },
            onConfirm = { health, notes ->
                viewModel.updateFlockletStatus(flockletToEdit, health, notes)
                showEditDialog = null
            }
        )
    }

    // Select Flock Dialog for Graduation
    showSelectFlockDialog?.let { flockletToGraduate ->
        SelectFlockDialog(
            flocks = availableFlocks,
            onDismiss = { showSelectFlockDialog = null },
            onConfirm = { targetFlockId ->
                viewModel.graduateFlocklet(flockletToGraduate, targetFlockId)
                showSelectFlockDialog = null
            },
            onAutoFlock = {
                // Determine targetFlockId = null for Auto
                viewModel.graduateFlocklet(flockletToGraduate, -1L)
                showSelectFlockDialog = null
            },
            onNewFlock = {
                onNavigateToAddFlock(
                    flockletToGraduate.species,
                    flockletToGraduate.breeds,
                    flockletToGraduate.id
                )
                showSelectFlockDialog = null
            }
        )
    }

    // Record Loss Dialog
    showRecordLossDialog?.let { flockletToUpdate ->
        var quantity by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRecordLossDialog = null },
            title = { Text(stringResource(NurR.string.record_loss_death_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(NurR.string.recording_loss_for_species_chicks, flockletToUpdate.species))
                    Text(
                        stringResource(NurR.string.remaining_count_format, flockletToUpdate.chickCount),
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                        label = { Text(stringResource(NurR.string.quantity_lost_label)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(stringResource(NurR.string.reason_example_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (quantity.toIntOrNull() == flockletToUpdate.chickCount) {
                        Text(
                            stringResource(NurR.string.warning_loss_removes_batch),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val qty = quantity.toIntOrNull() ?: 0
                        if (qty > 0 && qty <= flockletToUpdate.chickCount) {
                            viewModel.recordLoss(flockletToUpdate, qty, reason)
                            showRecordLossDialog = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(NurR.string.confirm_loss_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordLossDialog = null }) {
                    Text(stringResource(NurR.string.cancel_action))
                }
            }
        )
    }

    // Action Sheet Definition
    selectedRowForAction?.let { rowModel ->
        val orig = rowModel.originalFlocklet
        val actionModel = StandardActionModel(
            primaryActions = listOf(
                ActionItem(
                    label = UiText.DynamicString("Graduate / Move"),
                    icon = Icons.Default.Check,
                    onClick = { showSelectFlockDialog = orig }
                )
            ),
            secondaryActions = listOf(
                ActionItem(
                    label = UiText.DynamicString("Edit Status"),
                    icon = Icons.Default.Edit,
                    onClick = { showEditDialog = orig }
                ),
                ActionItem(
                    label = UiText.DynamicString("Sell Birds"),
                    icon = Icons.Default.ShoppingCart,
                    onClick = { onSellFlocklet(orig.syncId, "flocklet") }
                )
            ),
            financeActions = if (canAccessFinance) listOf(
                ActionItem(
                    label = UiText.DynamicString("Add Cost"),
                    icon = Icons.Default.Add,
                    onClick = { onAddFinancialEntry(orig.syncId, "flocklet", false) }
                ),
                ActionItem(
                    label = UiText.DynamicString("Add Revenue"),
                    icon = Icons.Default.Add,
                    onClick = { onAddFinancialEntry(orig.syncId, "flocklet", true) }
                )
            ) else emptyList(),
            destructiveActions = listOf(
                ActionItem(
                    label = UiText.DynamicString("Record Loss"),
                    icon = Icons.Default.Info, // Use info or warning icon
                    onClick = { showRecordLossDialog = orig }
                ),
                ActionItem(
                    label = UiText.DynamicString("Delete"),
                    icon = Icons.Default.Delete,
                    onClick = { showDeleteDialog = orig }
                )
            )
        )
        
        StandardActionSheet(
            title = UiText.DynamicString(rowModel.title),
            actionModel = actionModel,
            onDismiss = { selectedRowForAction = null },
            onOpenDetails = { onDeepdiveClick(orig.syncId, "flocklet") }
        )
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(UiR.string.nursery_flocklets_title), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(NurR.string.nursery_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                        }
                    },
                    actions = {
                        if (canAccessFinance) {
                            IconButton(onClick = { onAddFinancialEntry("shared", "flocklet", false) }) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = stringResource(UiR.string.add_cost_action),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onNavigateToEquipment) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = stringResource(com.example.hatchtracker.core.ui.R.string.my_devices_title)
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("AddFlockletFab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(NurR.string.add_manual_flocklet_desc))
                }
            }
        ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .testTag("NurseryScreen")
        ) {
            // Filter & Sort Bar
            StandardFilterBar(
                filters = listOf("All", "Ready", "Needs Attention"),
                selectedFilter = uiState.filter,
                onFilterSelected = { viewModel.updateFilter(it) }
            )
            
            if (uiState.items.isEmpty()) {
                if (uiState.filter == "All" && uiState.searchQuery.isBlank()) {
                    NurseryEmptyState(
                        hasCompletedIncubations = hasCompletedIncubations,
                        onAddFlocklet = { showAddDialog = true },
                        onGraduateIncubation = onNavigateToIncubation
                    )
                } else if (uiState.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(NurR.string.no_matches_found),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp) // minimal gap to look like standard lists
                ) {
                    // Render Sections
                    ListSection.values().sortedBy { it.priority }.forEach { section ->
                        val sectionItems = uiState.groupedItems[section] ?: emptyList()
                        if (sectionItems.isNotEmpty()) {
                            val isCollapsed = uiState.collapsedSections.contains(section)
                            
                            item(key = "header_${section.name}") {
                                UrgencySectionHeader(
                                    title = UiText.DynamicString(when (section) {
                                        ListSection.CRITICAL -> "Critical"
                                        ListSection.NEXT_7_DAYS -> "Next 7 Days"
                                        ListSection.STABLE -> "Stable"
                                        ListSection.ARCHIVED -> "Archived"
                                    }),
                                    isCollapsed = isCollapsed,
                                    count = sectionItems.size,
                                    onToggle = { viewModel.toggleSection(section) }
                                )
                            }
                            
                            if (!isCollapsed) {
                                items(sectionItems, key = { it.id }) { item ->
                                    val iconColor = if (item.statusText == "Ready") MaterialTheme.colorScheme.primary else Color.Gray

                                    CompactRowItem(
                                        title = UiText.DynamicString(item.title),
                                        subtitle = UiText.DynamicString(item.subtitle),
                                        statusText = item.statusText?.let { UiText.DynamicString(it) },
                                        iconColor = iconColor,
                                        onClick = { selectedRowForAction = item },
                                        onLongClick = { /* Future: Selection Mode */ }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog != null) {
        val toDelete = showDeleteDialog!!
        val manualDeletionReason = stringResource(NurR.string.manual_deletion_reason)
        var reason by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(NurR.string.delete_dialog_title)) },
            text = { 
                Column {
                    Text(stringResource(NurR.string.delete_dialog_message))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(stringResource(UiR.string.reason_optional_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeFlocklet(toDelete, reason.ifBlank { manualDeletionReason })
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(NurR.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(NurR.string.cancel_action))
                }
            }
        )
    }

    // Sell Dialog (New)
    // Intercept generic sell click to show local dialog instead of navigating
    // Only if we want to use the new "markSold" logic here.
    // The `onSellFlocklet` callback was passed from parent.
    // If we want to use local logic, we need to invoke it here.
    // But `FlockletCard` calls `onSellClick`.
    // I need to change `onSellClick` implementation in `items` loop below.

}
}

@Composable
fun SelectFlockDialog(
    flocks: List<com.example.hatchtracker.data.models.Flock>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onAutoFlock: () -> Unit,
    onNewFlock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(NurR.string.graduate_to_adult_flock_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(NurR.string.graduate_to_adult_flock_message),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // New Flock Button (Top)
                OutlinedButton(
                    onClick = onNewFlock,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(NurR.string.design_new_flock_custom))
                }

                // Auto Flock Button
                FilledTonalButton(
                    onClick = onAutoFlock,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(NurR.string.quick_graduate_auto_create))
                }

                if (flocks.isEmpty()) {
                    Text(
                        stringResource(NurR.string.no_flocks_found), 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(NurR.string.or_select_existing), 
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                    ) {
                        items(flocks, key = { it.id }) { flock ->
                            Surface(
                                onClick = { onConfirm(flock.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(flock.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                        Text("${flock.species} \u2022 ${flock.purpose}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(NurR.string.cancel_action))
            }
        }
    )
}

@Composable
fun NurseryEmptyState(
    hasCompletedIncubations: Boolean,
    onAddFlocklet: () -> Unit,
    onGraduateIncubation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = com.example.hatchtracker.core.ui.R.drawable.hatchy_1),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.nursery_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.nursery_empty_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        

        if (hasCompletedIncubations) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onGraduateIncubation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.action_graduate_incubation))
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        AppCard(
             variant = AppCardVariant.SUBTLE,
             modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.nursery_lifecycle_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}



