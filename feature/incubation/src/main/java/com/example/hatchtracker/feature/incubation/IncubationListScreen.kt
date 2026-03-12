// LAYOUT COMPOSITION CONTRACT
// This screen conforms strictly to the pre-polish layout baseline.
// Do NOT alter container structure or component layout without explicit design approval.
package com.example.hatchtracker.feature.incubation

import com.example.hatchtracker.data.models.Incubation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.data.models.hatchRate
import com.example.hatchtracker.data.models.infertilityRate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.hatchtracker.core.ui.R as UiR

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.example.hatchtracker.core.domain.models.ActionItem
import com.example.hatchtracker.core.domain.models.StandardActionModel
import com.example.hatchtracker.core.ui.components.StandardActionSheet
import com.example.hatchtracker.core.ui.components.StandardFilterBar
import com.example.hatchtracker.core.ui.components.CompactRowItem
import com.example.hatchtracker.core.ui.components.UrgencySectionHeader
import com.example.hatchtracker.core.ui.components.ScreenScaffold
import com.example.hatchtracker.feature.incubation.models.IncubationRowModel
import com.example.hatchtracker.core.domain.models.ListSection
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.hatchtracker.model.UiText

import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant

import androidx.compose.ui.graphics.Color
import com.example.hatchtracker.feature.incubation.IncubationViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncubationListScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAddIncubationClick: () -> Unit = {},
    onIncubationClick: (Long) -> Unit = {},
    onAddFinancialEntry: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onRecordSale: (String, String) -> Unit = { _, _ -> },
    onNavigateToEquipment: () -> Unit = {},
    viewModel: IncubationViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()
    val caps by viewModel.currentCapabilities.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val isAdmin by UserAuthManager.isSystemAdmin.collectAsState()
    val isDeveloper by UserAuthManager.isDeveloper.collectAsState()
    val canAccessFinance = FeatureAccessPolicy
        .canAccess(FeatureKey.FINANCE, caps.tier, isAdmin || isDeveloper)
        .allowed

    var showDeleteDialog by remember { mutableStateOf<Incubation?>(null) }
    
    // State for Action Sheet
    var selectedRowForAction by remember { mutableStateOf<IncubationRowModel?>(null) }
    var showEditDialogFor by remember { mutableStateOf<Incubation?>(null) }

    if (showDeleteDialog != null) {
        val toDelete = showDeleteDialog!!
        var reason by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(UiR.string.delete_incubation_dialog_title)) },
            text = { 
                Column {
                    Text(stringResource(UiR.string.delete_incubation_dialog_msg))
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
                val defaultReason = stringResource(UiR.string.manual_deletion_default_reason)
                TextButton(
                    onClick = {
                        viewModel.deleteIncubation(toDelete, reason.ifBlank { defaultReason })
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(UiR.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(UiR.string.cancel))
                }
            }
        )
    }

    // Action Sheet Definition
    selectedRowForAction?.let { rowModel ->
        val orig = rowModel.originalIncubation
        val actionModel = StandardActionModel(
            primaryActions = if (!orig.hatchCompleted) listOf(
                ActionItem(
                    label = UiText.DynamicString("Graduate / Move"),
                    icon = Icons.Default.Check,
                    onClick = { /* Navigate to Hatch outcome/graduation */ }
                )
            ) else emptyList(),
            secondaryActions = buildList {
                add(
                    ActionItem(
                        label = UiText.DynamicString("Edit Date & Device"),
                        icon = Icons.Default.Edit,
                        onClick = { showEditDialogFor = orig }
                    )
                )
                if (!orig.hatchCompleted) {
                    add(
                        ActionItem(
                            label = UiText.DynamicString("Sell Eggs"),
                            icon = Icons.Default.ShoppingCart,
                            onClick = { onRecordSale(orig.id.toString(), "incubation") }
                        )
                    )
                }
            },
            financeActions = if (canAccessFinance) listOf(
                ActionItem(
                    label = UiText.DynamicString("Add Cost"),
                    icon = Icons.Default.AddCircle,
                    onClick = { onAddFinancialEntry(orig.id.toString(), "incubation", false) }
                ),
                ActionItem(
                    label = UiText.DynamicString("Add Revenue"), // Unlikely for incubation, but present for symmetry
                    icon = Icons.Default.AddCircle,
                    onClick = { onAddFinancialEntry(orig.id.toString(), "incubation", true) }
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
            onOpenDetails = { onIncubationClick(orig.id) }
        )
    }

    if (showEditDialogFor != null) {
        val incubation = showEditDialogFor!!
        val allDevices by viewModel.allDevices.collectAsState()
        EditIncubationDialog(
            incubation = incubation,
            devices = allDevices,
            localeFormatService = viewModel.localeFormatService,
            dateFormat = dateFormat,
            onDismiss = { showEditDialogFor = null },
            onConfirm = { startDate, eggsCount, notes, deviceId ->
                viewModel.updateIncubation(incubation, startDate, eggsCount, notes, deviceId)
                showEditDialogFor = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.incubation_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                },
                actions = {
                    if (canAccessFinance) {
                        IconButton(onClick = { onAddFinancialEntry("shared", "incubation", false) }) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = stringResource(UiR.string.add_cost_content_description),
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
            val context = LocalContext.current
            FloatingActionButton(onClick = {
                if (viewModel.canCreateMoreIncubations(uiState.items.size)) {
                    onAddIncubationClick()
                } else {
                    android.widget.Toast.makeText(context, context.getString(UiR.string.incubation_limit_reached), android.widget.Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Default.AddCircle, contentDescription = stringResource(UiR.string.add_incubation_title))
            }
        }
    ) { paddingValues ->
        ScreenScaffold(modifier = modifier.fillMaxSize(), contentPadding = paddingValues) { safePadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(safePadding)
            ) {
                val hubSummary by viewModel.hubSummary.collectAsState()
                if (hubSummary.batchCount > 0) {
                    com.example.hatchtracker.feature.incubation.components.IncubationHubSummaryCard(
                        summary = hubSummary,
                        currencyCode = "USD", // TODO: Get from settings
                        localeFormatService = viewModel.localeFormatService,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Filter & Sort Bar
                StandardFilterBar(
                    filters = listOf("All", "Active", "Completed"),
                    selectedFilter = uiState.filter,
                    onFilterSelected = { viewModel.updateFilter(it) }
                )

                // List of incubations
                if (uiState.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(UiR.string.no_incubations_msg),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
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
                                        val iconColor = if (item.isCompleted) Color.Gray else MaterialTheme.colorScheme.tertiary
    
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
        }
    }
    }

@Deprecated("Replaced by CompactRowItem and StandardActionSheet")
@Composable
fun IncubationListItem(
    incubation: Incubation,
    birds: List<com.example.hatchtracker.data.models.Bird>,
    localeFormatService: com.example.hatchtracker.common.format.LocaleFormatService,
    dateFormat: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onSellClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    financialSummary: com.example.hatchtracker.data.models.FinancialSummary? = null,
    showFinancials: Boolean = false,
    onAddCostClick: () -> Unit = {},
    onAddRevenueClick: () -> Unit = {}
) {
    // Find parent bird
    val birdId = incubation.birdId
    val flockId = incubation.flockId
    val parentBird = birds.find { it.id == birdId }
    val parentBirdName = if (flockId != null) {
        stringResource(UiR.string.flock_batch_id_format, flockId.toString())
    } else {
        parentBird?.let { "${it.species} - ${it.breed}" }
            ?: stringResource(UiR.string.unknown_bird_id_format, birdId?.toString() ?: "")
    }
    
    // Calculate days until expected hatch
    val daysRemaining = remember(incubation.expectedHatch) {
        try {
            val today = todayUtcMillis()
            val hatchDate = parseIsoDateUtcMillis(incubation.expectedHatch)
            if (hatchDate != null) daysBetweenUtc(today, hatchDate) else 0
        } catch (e: Exception) {
            0
        }
    }
    
    // Calculate progress
    val progress = remember(incubation.startDate, incubation.expectedHatch) {
        try {
            val start = parseIsoDateUtcMillis(incubation.startDate)
            val end = parseIsoDateUtcMillis(incubation.expectedHatch)
            val today = todayUtcMillis()
            if (start == null || end == null) {
                0f
            } else {
                val totalDays = daysBetweenUtc(start, end).toFloat()
            if (totalDays <= 0) 1f
            else {
                    val elapsed = daysBetweenUtc(start, today).toFloat()
                (elapsed / totalDays).coerceIn(0f, 1f)
                }
            }
        } catch (e: Exception) {
            0f
        }
    }
    
    var showMenu by remember { mutableStateOf(false) }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        variant = if (incubation.hatchCompleted) AppCardVariant.SUBTLE else AppCardVariant.STANDARD,
        colors = if (incubation.hatchCompleted) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Parent Bird and Species
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parentBirdName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(UiR.string.current_stage_label, stringResource(UiR.string.incubation_stage)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(UiR.string.species_breeds_format, incubation.species, incubation.breeds.joinToString(", ")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (incubation.hatchCompleted) {
                    SuggestionChip(
                        onClick = { /* View Details */ },
                        label = { Text(stringResource(UiR.string.hatch_completed_chip), style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                
                // Action Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(UiR.string.options_content_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(UiR.string.edit_action)) },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        if (!incubation.hatchCompleted) {
                            DropdownMenuItem(
                                text = { Text(stringResource(UiR.string.sell_eggs_action)) },
                                onClick = {
                                    showMenu = false
                                    onSellClick()
                                },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(UiR.string.delete_action)) },
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
            
            HorizontalDivider()
            
            if (!incubation.hatchCompleted) {
                // Dates for active incubations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(UiR.string.start_date_required_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = localeFormatService.formatDate(incubation.startDate, dateFormat),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(UiR.string.expected_hatch_date_required_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = localeFormatService.formatDate(incubation.expectedHatch, dateFormat),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                val today = todayUtcMillis()
                val startDate = parseIsoDateUtcMillis(incubation.startDate)
                val daysUntilStart = if (startDate != null) daysBetweenUtc(today, startDate).toLong() else 0L
                val isScheduled = daysUntilStart > 0

                // Progress bar and status
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isScheduled) {
                        // Scheduled State
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (daysUntilStart == 1L)
                                    stringResource(UiR.string.starts_in_days_one, daysUntilStart)
                                else
                                    stringResource(UiR.string.starts_in_days_other, daysUntilStart),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "0%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        // Active State
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (daysRemaining > 0) {
                                    if (daysRemaining == 1) stringResource(UiR.string.days_remaining_one, daysRemaining)
                                    else stringResource(UiR.string.days_remaining_other, daysRemaining)
                                } else if (daysRemaining == 0) {
                                    stringResource(UiR.string.hatch_day_msg)
                                } else {
                                    val overdue = -daysRemaining
                                    if (overdue == 1) stringResource(UiR.string.overdue_days_one, overdue)
                                    else stringResource(UiR.string.overdue_days_other, overdue)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    daysRemaining < 0 -> MaterialTheme.colorScheme.error
                                    daysRemaining <= 3 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = when {
                                daysRemaining < 0 -> MaterialTheme.colorScheme.error
                                daysRemaining <= 3 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            } else {
                // Statistics for completed hatches
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(UiR.string.hatch_statistics_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(stringResource(UiR.string.total_eggs_label), incubation.eggsCount.toString())
                        StatItem(stringResource(UiR.string.hatched_label), incubation.hatchedCount.toString())
                        val rate = localeFormatService.formatPercent(incubation.hatchRate() / 100.0)
                        StatItem(stringResource(UiR.string.hatch_rate_label), rate)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(stringResource(UiR.string.infertile_label), incubation.infertileCount.toString())
                        StatItem(stringResource(UiR.string.failed_label), incubation.failedCount.toString())
                        val survivalRate = localeFormatService.formatPercent((100f - incubation.infertilityRate()) / 100.0)
                        StatItem(stringResource(UiR.string.survival_label), survivalRate)
                    }
                }
            }
            
            if (showFinancials) {
                 HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                 
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     Column {
                        financialSummary?.let { summary ->
                            val formattedProfit = localeFormatService.formatCurrency(summary.profit, "USD")
                            Text(
                                text = stringResource(UiR.string.profit_format, formattedProfit),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.profit >= 0) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFC62828)
                            )
                        } ?: Text(stringResource(UiR.string.no_financial_data_msg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    
                    Row {
                         IconButton(onClick = onAddCostClick) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = stringResource(UiR.string.add_cost_content_description), tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = onAddRevenueClick) {
                            Icon(Icons.Default.AddCircle, contentDescription = stringResource(UiR.string.add_revenue_content_description), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private fun parseIsoDateUtcMillis(value: String): Long? {
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        parser.parse(value)?.time
    }.getOrNull()
}

private fun todayUtcMillis(): Long {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun daysBetweenUtc(startMillis: Long, endMillis: Long): Int {
    return ((endMillis - startMillis) / MILLIS_PER_DAY).toInt()
}
