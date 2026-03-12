// LAYOUT COMPOSITION CONTRACT
// This screen conforms strictly to the pre-polish layout baseline.
// Do NOT alter container structure or component layout without explicit design approval.
package com.example.hatchtracker.feature.flock.ui.screens

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import com.example.hatchtracker.core.ui.composeutil.premiumClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.hatchtracker.feature.flock.BuildConfig

import com.example.hatchtracker.core.domain.models.ActionItem
import com.example.hatchtracker.core.domain.models.StandardActionModel
import com.example.hatchtracker.core.ui.components.StandardActionSheet
import com.example.hatchtracker.core.ui.components.StandardFilterBar
import com.example.hatchtracker.core.ui.components.ProfileCompactRowItem
import com.example.hatchtracker.core.ui.components.UrgencySectionHeader
import com.example.hatchtracker.feature.flock.models.FlockRowModel
import com.example.hatchtracker.core.domain.models.ListSection

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.feature.flock.ui.viewmodels.FlockViewModel
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.common.format.LocaleFormatService
import com.example.hatchtracker.model.UiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlockListScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onFlockClick: (Long) -> Unit = {},
    onAddFlockClick: () -> Unit = {},
    onAddFinancialEntry: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onRecordSale: (String, String) -> Unit = { _, _ -> },
    onNavigateToEquipment: () -> Unit = {},
    viewModel: FlockViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val caps by viewModel.currentCapabilities.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val isAdmin by UserAuthManager.isSystemAdmin.collectAsState()
    val isDeveloper by UserAuthManager.isDeveloper.collectAsState()
    val canAccessFinance = FeatureAccessPolicy
        .canAccess(FeatureKey.FINANCE, caps.tier, isAdmin || isDeveloper)
        .allowed

    var showDeleteDialog by remember { mutableStateOf<Flock?>(null) }
    var showEditDialogFor by remember { mutableStateOf<Flock?>(null) }
    var selectedRowForAction by remember { mutableStateOf<FlockRowModel?>(null) }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.remove_flock_confirm_title)) },
            text = { Text(stringResource(R.string.remove_flock_confirm_message, showDeleteDialog?.name ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val flockToDelete = showDeleteDialog
                        if (flockToDelete != null) {
                            viewModel.deleteFlock(flockToDelete)
                        }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEditDialogFor != null) {
        val flock = showEditDialogFor!!
        EditFlockDialog(
            flock = flock,
            onDismiss = { showEditDialogFor = null },
            onConfirm = { name, notes, purpose, active, breeds ->
                viewModel.updateFlock(flock, name, notes, purpose, active, breeds)
                showEditDialogFor = null
            }
        )
    }

    // Action Sheet Definition
    selectedRowForAction?.let { rowModel ->
        val orig = rowModel.originalFlock
        val actionModel = StandardActionModel(
            primaryActions = emptyList(), 
            secondaryActions = buildList {
                add(
                    ActionItem(
                        label = UiText.DynamicString("Edit details"),
                        icon = Icons.Default.Edit,
                        onClick = { showEditDialogFor = orig }
                    )
                )
                if (orig.active) {
                    add(
                        ActionItem(
                            label = UiText.DynamicString("Sell Birds"),
                            icon = Icons.Default.ShoppingCart,
                            onClick = { onRecordSale(orig.syncId, "flock") }
                        )
                    )
                }
            },
            financeActions = if (canAccessFinance) listOf(
                ActionItem(
                    label = UiText.DynamicString("Add Cost"),
                    icon = Icons.Default.Add,
                    onClick = { onAddFinancialEntry(orig.syncId, "flock", false) }
                ),
                ActionItem(
                    label = UiText.DynamicString("Add Revenue"),
                    icon = Icons.Default.Add,
                    onClick = { onAddFinancialEntry(orig.syncId, "flock", true) }
                )
            ) else emptyList(),
            destructiveActions = listOf(
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
            onOpenDetails = { onFlockClick(orig.id) }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.your_flocks_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_action))
                    }
                },
                actions = {
                    if (canAccessFinance) {
                        IconButton(onClick = { onAddFinancialEntry("shared", "flock", false) }) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = stringResource(R.string.add_cost_action),
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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFlockClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_flock_action))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StandardFilterBar(
                filters = listOf("All", "Active", "Archived"),
                selectedFilter = uiState.filter,
                onFilterSelected = { viewModel.updateFilter(it) }
            )

            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_flocks_empty_state),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    ListSection.values().sortedBy { it.priority }.forEach { section ->
                        val sectionItems = uiState.groupedItems[section] ?: emptyList()
                        if (sectionItems.isNotEmpty()) {
                            val isCollapsed = uiState.collapsedSections.contains(section)
                            
                            item(key = "header_${section.name}") {
                                UrgencySectionHeader(
                                    title = UiText.DynamicString(
                                        when (section) {
                                            ListSection.CRITICAL -> "Critical"
                                            ListSection.NEXT_7_DAYS -> "Next 7 Days"
                                            ListSection.STABLE -> "Stable"
                                            ListSection.ARCHIVED -> "Archived"
                                        }
                                    ),
                                    isCollapsed = isCollapsed,
                                    count = sectionItems.size,
                                    onToggle = { viewModel.toggleSection(section) }
                                )
                            }
                            
                            if (!isCollapsed) {
                                items(sectionItems, key = { it.id }) { item ->
                                    val iconColor = if (item.isArchived) androidx.compose.ui.graphics.Color.Gray else MaterialTheme.colorScheme.primary

                                    ProfileCompactRowItem(
                                        title = UiText.DynamicString(item.title),
                                        subtitle = UiText.DynamicString(item.subtitle),
                                        imagePath = item.originalFlock.imagePath,
                                        species = item.originalFlock.species,
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
    }
}

@Deprecated("Replaced by CompactRowItem and StandardActionSheet")
@Composable
fun FlockCard(
    flock: Flock,
    financialSummary: com.example.hatchtracker.data.models.FinancialSummary? = null,
    showFinancials: Boolean = false,
    formatService: LocaleFormatService,
    currencyCode: String,
    dateFormat: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSellClick: () -> Unit = {},
    onEditClick: () -> Unit,
    onAddCostClick: () -> Unit = {},
    onAddRevenueClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .premiumClickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = flock.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.options_content_description),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_action)) },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sell_birds_action)) },
                            onClick = {
                                showMenu = false
                                onSellClick()
                            },
                            leadingIcon = { Icon(Icons.Default.ShoppingCart, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_action)) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                 Text(
                    text = flock.species.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = flock.purpose.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            if (showFinancials) {
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        financialSummary?.let { summary ->
                            Text(
                                text = stringResource(R.string.profit_label_format, formatService.formatCurrency(summary.profit, currencyCode)),
                                style = MaterialTheme.typography.bodyEmphasis,
                                color = if (summary.profit >= 0) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFC62828)
                            )
                        } ?: Text(stringResource(R.string.no_financial_data), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    
                    Row {
                         IconButton(onClick = onAddCostClick) {
                            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.add_cost_action), tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = onAddRevenueClick) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_revenue_action), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = stringResource(R.string.created_at_label_format, formatService.formatDate(flock.createdAt, dateFormat)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
