package com.example.hatchtracker.feature.incubation

import androidx.compose.foundation.layout.*
import com.example.hatchtracker.data.models.Incubation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.core.ui.components.IncubationStatusCard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.common.R as ComR
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncubationDetailScreen(
    incubationId: Long,
    viewModel: IncubationDetailViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAddFinancialEntry: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onRecordHatchClick: (Long) -> Unit = {},
    onRecordSale: (String, String) -> Unit = { _, _ -> },
    onNavigateToTroubleshooting: (Long) -> Unit = {},
    onDeepdiveClick: (String, String) -> Unit = { _, _ -> }
) {
    val incubation by viewModel.incubation.collectAsState()
    val financialSummary by viewModel.financialSummary.collectAsState()
    val caps by viewModel.capabilities.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val timeFormat by viewModel.timeFormat.collectAsState()
    val hatchyAdvice by viewModel.hatchyAdvice.collectAsState()
    val isAdmin by UserAuthManager.isSystemAdmin.collectAsState()
    val isDeveloper by UserAuthManager.isDeveloper.collectAsState()
    val fertilityRate by viewModel.fertilityRate.collectAsState()
    val hatchability by viewModel.hatchability.collectAsState()
    val canAccessFinance = FeatureAccessPolicy
        .canAccess(FeatureKey.FINANCE, caps.tier, isAdmin || isDeveloper)
        .allowed

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAdviceDialog by remember { mutableStateOf(false) }

    if (showAdviceDialog && hatchyAdvice != null) {
        val adviceText = when(hatchyAdvice) {
            "hatchy_advice_lockdown" -> stringResource(ComR.string.hatchy_advice_lockdown)
            "hatchy_advice_approaching" -> stringResource(ComR.string.hatchy_advice_approaching)
            "hatchy_advice_mid_incubation" -> stringResource(ComR.string.hatchy_advice_mid_incubation)
            "hatchy_advice_early" -> stringResource(ComR.string.hatchy_advice_early)
            else -> hatchyAdvice!!
        }
        AlertDialog(
            onDismissRequest = { showAdviceDialog = false },
            title = { Text(stringResource(UiR.string.hatchy_tip_title)) },
            text = { Text(adviceText) },
            confirmButton = {
                TextButton(onClick = { showAdviceDialog = false }) {
                    Text(stringResource(UiR.string.got_it_action))
                }
            }
        )
    }

    if (showEditDialog && incubation != null) {
        val allDevices by viewModel.allDevices.collectAsState()
        EditIncubationDialog(
            incubation = incubation!!,
            devices = allDevices,
            localeFormatService = viewModel.localeFormatService,
            dateFormat = dateFormat,
            onDismiss = { showEditDialog = false },
            onConfirm = { startDate, eggsCount, notes, deviceId ->
                viewModel.updateIncubation(startDate, eggsCount, notes, deviceId)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && incubation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(UiR.string.remove_batch_confirm_title)) },
            text = { Text(stringResource(UiR.string.remove_batch_confirm_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteIncubation()
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(UiR.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(UiR.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.incubation_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.back_action))
                    }
                },
                actions = {
                    // Advice Icon
                    IconButton(onClick = { showAdviceDialog = true }) {
                        Icon(Icons.Default.Info, stringResource(UiR.string.hatchy_tip_title), tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    // Options Menu
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(UiR.string.options_content_description))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(UiR.string.edit_action)) },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            if (incubation?.hatchCompleted == false) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(UiR.string.sell_eggs_action)) },
                                    onClick = {
                                        showMenu = false
                                        incubation?.let { onRecordSale(it.id.toString(), "incubation") }
                                    },
                                    leadingIcon = { Icon(Icons.Default.ShoppingCart, null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(UiR.string.delete_action)) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 incubation?.let {
                    Column {
                        Text(
                            text = stringResource(UiR.string.batch_header_format, it.species),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(UiR.string.incubation_started_format, viewModel.localeFormatService.formatDate(it.startDate, dateFormat)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (incubation == null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val currentIncubation = incubation!!
                
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Dynamic Timeline Visualization
                    // Resolve device features for timeline
                    val allDevices by viewModel.allDevices.collectAsState()
                    val activeDevice = allDevices.find { it.id == currentIncubation.incubatorDeviceId }
                    
                    com.example.hatchtracker.core.ui.components.IncubationTimelineView(
                        incubation = currentIncubation,
                        deviceFeatures = activeDevice?.features
                    )

                    // Financial Summary Card
                    val summary = financialSummary?.let { stats ->
                        com.example.hatchtracker.data.models.FinancialSummary(
                            summaryId = "incubation_${currentIncubation.id}",
                            ownerType = "incubation",
                            ownerId = currentIncubation.id.toString(),
                            totalCosts = stats.totalCost,
                            totalRevenue = stats.totalRevenue,
                            profit = stats.netProfit
                        )
                    }

                    if (summary != null) {
                        com.example.hatchtracker.core.ui.components.FinancialSummaryCard(
                            summary = summary,
                            currencyCode = currencyCode,
                            onAddCostClick = { 
                                onAddFinancialEntry(currentIncubation.id.toString(), "incubation", false) 
                            },
                            onAddRevenueClick = { 
                                onAddFinancialEntry(currentIncubation.id.toString(), "incubation", true) 
                            },
                            onDeepdiveClick = {
                                onDeepdiveClick(currentIncubation.id.toString(), "incubation")
                            },
                            tier = caps.tier,
                            isAdmin = isAdmin,
                            isDeveloper = isDeveloper
                        )

                        // Pain-to-Pay Trigger: Financial Insights
                        val insights by viewModel.financialInsights.collectAsState()
                        val isPro = caps.tier == com.example.hatchtracker.data.models.SubscriptionTier.PRO
                        
                        FinancialInsightsCard(
                            insights = insights,
                            isPro = isPro,
                            currencyCode = currencyCode,
                            localeFormatService = viewModel.localeFormatService,
                            onUnlockClick = { 
                                // In a real app, navigate to SubscriptionScreen
                                // For now, we'll assume the user needs to visit Settings/Upgrade
                            }
                        )
                    }

                    // Other details (Eggs count, etc.)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(UiR.string.batch_info_header), style = MaterialTheme.typography.titleMedium)
                            Text("${stringResource(UiR.string.total_eggs_label)}: ${currentIncubation.eggsCount}")
                            

                            Text("${stringResource(UiR.string.expected_hatch_date_label)}: ${viewModel.localeFormatService.formatDate(currentIncubation.expectedHatch, dateFormat)}")
                            val flockId = currentIncubation.flockId
                            val birdId = currentIncubation.birdId
                            if (flockId != null) {
                                Text(stringResource(UiR.string.linked_to_flock_id_format, flockId.toString()))
                            } else if (birdId != null) {
                                Text(stringResource(UiR.string.mother_bird_id_format, birdId.toString()))
                            }
                            
                            // Notes with edit hint
                            currentIncubation.notes?.let { noteText ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(UiR.string.notes_label_detail), style = MaterialTheme.typography.labelSmall)
                                Text(noteText, style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            // Device Info
                            val incubator = allDevices.find { it.id == currentIncubation.incubatorDeviceId }
                            val hatcher = allDevices.find { it.id == currentIncubation.hatcherDeviceId }
                            
                            if (incubator != null) {
                                Text(stringResource(UiR.string.incubator_label_format, incubator.displayName))
                            }
                            if (hatcher != null) {
                                Text(stringResource(UiR.string.hatcher_label_format, hatcher.displayName))
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedButton(
                                onClick = { onNavigateToTroubleshooting(currentIncubation.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(UiR.string.troubleshoot_conditions_action))
                            }
                        }
                    }

                    val performanceState by viewModel.performanceState.collectAsState()
                    
                    if (performanceState != null && performanceState!!.isCompleted && currentIncubation.hatchCompleted) {
                        com.example.hatchtracker.feature.incubation.components.IncubationPerformanceCard(
                            fertilityRate = performanceState!!.fertilityRate,
                            hatchability = performanceState!!.hatchability,
                            costPerHatch = performanceState!!.costPerHatch,
                            currencyCode = currencyCode, // TODO: Get from settings
                            localeFormatService = viewModel.localeFormatService,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (!currentIncubation.hatchCompleted) {
                        val status = com.example.hatchtracker.core.common.IncubationManager.getStatus(currentIncubation)
                        
                        // Hatcher Migration Card
                        if (status.phase >= com.example.hatchtracker.core.common.IncubationPhase.LOCKDOWN && currentIncubation.incubatorDeviceId.isNotEmpty() && currentIncubation.hatcherDeviceId.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(stringResource(UiR.string.lockdown_action_title), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(UiR.string.lockdown_action_msg), style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    val hatchers by viewModel.userHatchers.collectAsState()
                                    var expanded by remember { mutableStateOf(false) }
                                    
                                    // Move to Hatcher Button (Dropdown)
                                    Box {
                                        Button(onClick = { expanded = true }) {
                                            Text(stringResource(UiR.string.move_to_hatcher_action))
                                        }
                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            hatchers.filter { it.device.type == com.example.hatchtracker.data.models.DeviceType.HATCHER }.forEach { capacity ->
                                                DropdownMenuItem(
                                                    text = { Text("${capacity.device.displayName} (${capacity.remainingCapacity} free)") },
                                                    onClick = { 
                                                        viewModel.assignHatcher(capacity.device.id, keepInIncubator = false)
                                                        expanded = false 
                                                    }
                                                )
                                            }
                                            if (hatchers.none { it.device.type == com.example.hatchtracker.data.models.DeviceType.HATCHER }) {
                                                DropdownMenuItem(text = { Text(stringResource(UiR.string.no_active_hatchers_found)) }, onClick = { expanded = false }, enabled = false)
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(onClick = { viewModel.assignHatcher(null, keepInIncubator = true) }) {
                                        Text(stringResource(UiR.string.keep_in_current_device_action))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { viewModel.assignHatcher(null, keepInIncubator = false) }) {
                                        Text(stringResource(UiR.string.unlink_device_action))
                                    }
                                }
                            }
                        }

                        // Hatch Record Button logic
                        val showRecordButton = status.phase in listOf(
                            com.example.hatchtracker.core.common.IncubationPhase.LOCKDOWN,
                            com.example.hatchtracker.core.common.IncubationPhase.HATCH_WINDOW,
                            com.example.hatchtracker.core.common.IncubationPhase.OVERDUE
                        )

                        if (showRecordButton) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onRecordHatchClick(currentIncubation.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(UiR.string.record_hatch_outcome_action))
                            }
                        }
                    }
                }
            }
        }
    }
}
