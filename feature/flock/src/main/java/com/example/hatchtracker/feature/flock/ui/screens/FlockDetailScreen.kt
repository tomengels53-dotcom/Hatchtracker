package com.example.hatchtracker.feature.flock.ui.screens

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.example.hatchtracker.core.ui.composeutil.premiumCombinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.common.format.LocaleFormatService
import com.example.hatchtracker.core.common.asString
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant
import com.example.hatchtracker.core.ui.components.FinancialSummaryCard

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.feature.flock.ui.viewmodels.FlockDetailViewModel
import com.example.hatchtracker.core.ui.components.ProfileHeroHeader
import com.example.hatchtracker.core.ui.util.rememberProfileImageState
import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.core.ui.R as UiR
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlockDetailScreen(
    viewModel: FlockDetailViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAddBirdClick: (Long, String) -> Unit = { _, _ -> },
    onStartIncubationClick: (Long, String) -> Unit = { _, _ -> },
    onRecordSale: (String, String) -> Unit = { _, _ -> },
    onSellBirds: (String, String, List<Long>) -> Unit = { _, _, _ -> },
    onAddFinancialEntry: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onLogProduction: (String) -> Unit = { _ -> },
    onBirdClick: (Long) -> Unit = {},
    onDeepdiveClick: (String, String) -> Unit = { _, _ -> }
) {
    val flock by viewModel.flock.collectAsState()
    val birds by viewModel.birds.collectAsState()
    val financialSummary by viewModel.financialSummary.collectAsState()
    val caps by viewModel.capabilities.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val selectedBirds by viewModel.selectedBirds.collectAsState()
    val allFlocks by viewModel.allFlocks.collectAsState()
    val hatchyAdvice by viewModel.hatchyAdvice.collectAsState()
    val eggProductionStats by viewModel.eggProductionStats.collectAsState()
    val isAdmin by UserAuthManager.isSystemAdmin.collectAsState()
    val isDeveloper by UserAuthManager.isDeveloper.collectAsState()

    val canAccessFinance = FeatureAccessPolicy
        .canAccess(FeatureKey.FINANCE, caps.tier, isAdmin || isDeveloper)
        .allowed

    var showMoveDialog by remember { mutableStateOf(false) }
    var showAdviceDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRecordLossDialog by remember { mutableStateOf(false) }
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf(
        stringResource(UiR.string.overview_tab),
        stringResource(UiR.string.birds_tab),
        stringResource(UiR.string.production_tab),
        stringResource(UiR.string.finance_tab)
    )


    if (showMoveDialog) {
        SelectFlockDialog(
            flocks = allFlocks.filter { it.id != flock?.id },
            onDismiss = { showMoveDialog = false },
            onConfirm = { targetId ->
                viewModel.moveSelectedBirds(targetId)
                showMoveDialog = false
            },
            onAutoFlock = { showMoveDialog = false },
            onNewFlock = { showMoveDialog = false }
        )
    }

    if (showAdviceDialog && hatchyAdvice != null) {
        AlertDialog(
            onDismissRequest = { showAdviceDialog = false },
            title = { Text(stringResource(UiR.string.hatchy_tip_title)) },
            text = { Text(hatchyAdvice?.asString() ?: "") },
            confirmButton = {
                TextButton(onClick = { showAdviceDialog = false }) { Text(stringResource(UiR.string.got_it_action)) }
            }
        )
    }

    if (showEditDialog && flock != null) {
        EditFlockDialog(
            flock = flock!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, notes, purpose, active, breeds ->
                viewModel.updateFlock(name, notes, purpose, active, breeds)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && flock != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(UiR.string.remove_flock_confirm_title)) },
            text = { Text(stringResource(UiR.string.remove_flock_confirm_message, flock?.name ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFlock()
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(UiR.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(UiR.string.cancel)) }
            }
        )
    }

    if (showRecordLossDialog) {
        val count = selectedBirds.size
        val recordLossReason = stringResource(UiR.string.record_loss_reason_default)
        AlertDialog(
            onDismissRequest = { showRecordLossDialog = false },
            title = { Text(stringResource(UiR.string.record_loss_title_format, count)) },
            text = { Text(stringResource(UiR.string.record_loss_selected_birds_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.recordBirdsDeath(selectedBirds.toList(), recordLossReason)
                        showRecordLossDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(UiR.string.confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showRecordLossDialog = false }) { Text(stringResource(UiR.string.cancel)) }
            }
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var tempPhotoPath by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoPath != null) {
            scope.launch {
                viewModel.saveProfilePhoto(android.net.Uri.fromFile(java.io.File(tempPhotoPath!!)))
                    .onFailure { snackbarHostState.showSnackbar("Failed to save photo") }
            }
        }
    }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            scope.launch {
                viewModel.saveProfilePhoto(it)
                    .onFailure { snackbarHostState.showSnackbar("Failed to save photo") }
            }
        }
    }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingCameraUri?.let { uri -> cameraLauncher.launch(uri) }
        }
    }

    val showPhotoPicker: () -> Unit = {
        // Simple dialog for camera vs gallery
        // For brevity in this non-interactive mock, assume we show a standard bottom sheet or dialog
    }

    Scaffold(
            topBar = {
                if (selectedBirds.isNotEmpty()) {
                    TopAppBar(
                        title = { Text(stringResource(UiR.string.selected_count_format, selectedBirds.size)) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(UiR.string.clear_selection_content_description))
                            }
                        },
                        actions = {
                            TextButton(onClick = { showMoveDialog = true }) { Text(stringResource(UiR.string.move_action)) }
                            TextButton(onClick = {
                                flock?.let { f ->
                                    onSellBirds(f.syncId, "flock", selectedBirds.toList())
                                    viewModel.clearSelection()
                                }
                            }) { Text(stringResource(UiR.string.sell_action)) }
                            IconButton(onClick = { showRecordLossDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(UiR.string.record_loss_content_description), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                } else {
                    var showMenu by remember { mutableStateOf(false) }
                    TopAppBar(
                        title = { Text("") }, // Title in HeroHeader
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                            }
                        },
                        actions = {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(UiR.string.options_content_description))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(UiR.string.edit_flock_action)) },
                                    onClick = { showMenu = false; showEditDialog = true },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(UiR.string.sell_birds_action)) },
                                    onClick = {
                                        showMenu = false
                                        flock?.let { onRecordSale(it.syncId, "flock") }
                                    },
                                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(UiR.string.delete_flock_action)) },
                                    onClick = { showMenu = false; showDeleteDialog = true },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (selectedBirds.isEmpty()) {
                    flock?.let { currentFlock ->
                        FloatingActionButton(onClick = { onAddBirdClick(currentFlock.id, currentFlock.species.name) }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(UiR.string.add_bird_content_description))
                        }
                    }
                }
            },
            snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                flock?.let { currentFlock ->
                    // Hero Header
                    val profileState = rememberProfileImageState(
                        imagePath = currentFlock.imagePath,
                        species = currentFlock.species
                    )
                    
                    ProfileHeroHeader(
                        state = profileState,
                        title = currentFlock.name,
                        subtitle = "${currentFlock.species.name} • ${currentFlock.breeds.joinToString(", ")} • ${currentFlock.purpose}",
                        onEditClick = {
                            // Standard photo picker logic (Camera/Gallery)
                            galleryLauncher.launch("image/*")
                        },
                        onRemoveClick = { viewModel.removeProfilePhoto() }
                    )

                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTabIndex) {
                            0 -> { // Overview
                                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onStartIncubationClick(currentFlock.id, currentFlock.syncId) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(UiR.string.start_incubation_action))
                                        }
                                        OutlinedButton(
                                            onClick = { onRecordSale(currentFlock.syncId, "flock") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(UiR.string.record_sale_action))
                                        }
                                    }

                                    currentFlock.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                                        AppCard(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            variant = AppCardVariant.STANDARD
                                        ) {
                                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(16.dp)) {
                                                IconButton(onClick = { showAdviceDialog = true }, modifier = Modifier.size(24.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = stringResource(UiR.string.show_advice_content_description),
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = notes, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> { // Birds
                                if (birds.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(stringResource(UiR.string.no_birds_in_flock_yet))
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            Text(
                                                text = stringResource(UiR.string.birds_count_format, birds.size),
                                                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                        items(birds) { bird ->
                                            val isSelected = selectedBirds.contains(bird.id)
                                            BirdItemCard(
                                                bird = bird,
                                                isSelected = isSelected,
                                                inSelectionMode = selectedBirds.isNotEmpty(),
                                                formatService = viewModel.localeFormatService,
                                                dateFormat = dateFormat,
                                                onClick = {
                                                    if (selectedBirds.isNotEmpty()) viewModel.toggleBirdSelection(bird.id)
                                                    else onBirdClick(bird.id)
                                                },
                                                onLongClick = { viewModel.toggleBirdSelection(bird.id) }
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> { // Production
                                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    OutlinedButton(
                                        onClick = { onLogProduction(currentFlock.syncId) },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Icon(Icons.Default.Egg, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(UiR.string.log_daily_production_action))
                                    }
                                    
                                    EggProductionOverviewCard(
                                        stats = eggProductionStats,
                                        modifier = Modifier.fillMaxWidth(),
                                        onLogClick = { onLogProduction(currentFlock.syncId) }
                                    )
                                }
                            }
                            3 -> { // Finance
                                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    if (canAccessFinance) {
                                        FinancialSummaryCard(
                                            summary = financialSummary,
                                            modifier = Modifier.fillMaxWidth(),
                                            currencyCode = currencyCode,
                                            onAddCostClick = { onAddFinancialEntry(currentFlock.syncId, "flock", false) },
                                            onAddRevenueClick = { onAddFinancialEntry(currentFlock.syncId, "flock", true) },
                                            onDeepdiveClick = { onDeepdiveClick(currentFlock.syncId, "flock") },
                                            tier = caps.tier,
                                            isAdmin = isAdmin,
                                            isDeveloper = isDeveloper
                                        )
                                    } else {
                                        // Simple placeholder if gated, though usually we might show basic ledger or an upsell
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(stringResource(UiR.string.financial_insights_pro_expert_only))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BirdItemCard(
    bird: Bird,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    formatService: LocaleFormatService,
    dateFormat: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    com.example.hatchtracker.core.ui.components.ProfileCompactRowItem(
        title = UiText.DynamicString("${bird.breed} ${bird.sex}"),
        subtitle = UiText.DynamicString(formatService.formatDate(bird.hatchDate, dateFormat)),
        imagePath = bird.imagePath,
        species = bird.species,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        trailingContent = {
            if (inSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            } else {
                bird.color?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    )
}

@Composable
private fun SelectFlockDialog(
    flocks: List<Flock>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onAutoFlock: () -> Unit,
    onNewFlock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(UiR.string.select_target_flock_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onNewFlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(UiR.string.design_new_flock_custom))
                }

                FilledTonalButton(
                    onClick = onAutoFlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(UiR.string.quick_graduate_auto_create))
                }

                if (flocks.isEmpty()) {
                    Text(
                        text = stringResource(UiR.string.no_existing_flocks_found),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(UiR.string.or_select_existing),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(flocks) { flock ->
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
                                        Text(
                                            text = flock.name,
                                            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                                        )
                                        Text(
                                            text = "${flock.species.name} • ${flock.purpose}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
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
            TextButton(onClick = onDismiss) { Text(stringResource(UiR.string.cancel)) }
        }
    )
}

@Composable
private fun EggProductionOverviewCard(
    stats: List<com.example.hatchtracker.data.models.EggProductionEntity>,
    modifier: Modifier = Modifier,
    onLogClick: () -> Unit
) {
    val last7Days = stats.take(7)
    val totalEggs = last7Days.sumOf { it.totalEggs }
    val avgEggs = if (last7Days.isNotEmpty()) totalEggs.toDouble() / last7Days.size else 0.0
    
    AppCard(
        modifier = modifier,
        variant = AppCardVariant.STANDARD
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(UiR.string.egg_production_last_7_days),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                )
                TextButton(onClick = onLogClick) {
                    Text(stringResource(UiR.string.log_more_action))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = totalEggs.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text(text = stringResource(UiR.string.total_eggs_label), style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "%.1f".format(avgEggs), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(text = stringResource(UiR.string.avg_per_day_label), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
