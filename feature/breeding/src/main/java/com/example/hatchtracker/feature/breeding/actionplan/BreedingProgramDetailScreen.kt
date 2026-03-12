package com.example.hatchtracker.feature.breeding.actionplan

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.R as UiR
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant

import com.example.hatchtracker.data.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingProgramDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBirdPicker: (String) -> Unit,
    onNavigateToLinkAssets: () -> Unit,
    onNavigateToSelectBirds: (Int) -> Unit,
    viewModel: BreedingProgramDetailViewModel = hiltViewModel()
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val genFinancials by viewModel.genFinancials.collectAsState()
    
    var showAddNoteDialog by remember { mutableStateOf(false) }

    Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(dashboardState?.program?.name ?: stringResource(UiR.string.breeding_action_plan_default_title))
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.back_action))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddNoteDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.NoteAdd, "Add Note")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    dashboardState?.let { state ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Dashboard Header: Status & Estimate
                            item {
                                DashboardHeader(
                                    health = state.program.programHealth,
                                    estimate = state.program.dynamicGenEstimate
                                )
                            }

                            // Current Stage & Next Actions
                            item {
                                CurrentStageCard(
                                    stage = state.currentStage,
                                    nextActions = state.nextActions,
                                    onToggleAction = { actionId, done ->
                                        state.currentStage?.let { stage ->
                                            viewModel.toggleAction(stage.stageId, actionId, done)
                                        }
                                    }
                                )
                            }

                            // Insights
                            if (state.whatsGood.isNotEmpty() || state.needsAttention.isNotEmpty()) {
                                item {
                                    InsightsCard(
                                        whatsGood = state.whatsGood,
                                        needsAttention = state.needsAttention,
                                        adjustments = state.suggestedAdjustments
                                    )
                                }
                            }

                            // Financial Overview for Active Generation
                            item {
                                genFinancials[state.program.activeGenerationIndex]?.let { 
                                    FinancialOverviewCard(it) 
                                }
                            }

                            // Stage Detail List
                            item {
                                Text(
                                    "Execution Roadmap",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            items(state.program.executionStages) { stage ->
                                StageRow(stage = stage)
                            }

                            // Recent Notes
                            if (state.program.executionNotes.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Recent Notes", style = MaterialTheme.typography.titleSmall)
                                }
                                items(state.program.executionNotes.sortedByDescending { it.createdAt }.take(5)) { note ->
                                    NoteRow(note)
                                }
                            }

                            // Debug Diagnostics
                            state.diagnostics?.let { diag ->
                                item {
                                    ExecutionDiagnosticsCard(diag)
                                }
                            }
                        }
                    }
                }
            }
        }
    }



@Composable
fun DashboardHeader(health: ProgramHealth?, estimate: GenEstimate?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppCard(modifier = Modifier.weight(1f), variant = AppCardVariant.STANDARD) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Program Health", style = MaterialTheme.typography.labelSmall)
                val status = health?.status ?: ProgramHealthStatus.INSUFFICIENT_DATA
                val color = when (status) {
                    ProgramHealthStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
                    ProgramHealthStatus.DRIFT -> Color.Yellow
                    ProgramHealthStatus.OFF_TRACK -> MaterialTheme.colorScheme.error
                    ProgramHealthStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.outline
                }
                Text(status.name, color = color, style = MaterialTheme.typography.titleMedium)
                Text("${health?.confidence ?: Confidence.LOW} Confidence", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        AppCard(modifier = Modifier.weight(1f), variant = AppCardVariant.STANDARD) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Gen Estimate", style = MaterialTheme.typography.labelSmall)
                val range = if (estimate != null) "${estimate.minGenerations}-${estimate.maxGenerations}" else "N/A"
                Text("$range Gens", style = MaterialTheme.typography.titleMedium)
                Text(estimate?.confidence?.name ?: "LOW", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun CurrentStageCard(stage: ExecutionStage?, nextActions: List<StageAction>, onToggleAction: (String, Boolean) -> Unit) {
    AppCard(variant = AppCardVariant.STANDARD) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stage?.titleKey ?: "Setup Preparation", style = MaterialTheme.typography.titleLarge)
            Text(stage?.descriptionKey ?: "Initialize program data.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            if (nextActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Next Steps", style = MaterialTheme.typography.titleSmall)
                nextActions.forEach { action ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = action.isDone, onCheckedChange = { onToggleAction(action.actionId, it) })
                        Text(action.labelKey, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun InsightsCard(whatsGood: List<String>, needsAttention: List<String>, adjustments: List<String>) {
    AppCard(variant = AppCardVariant.SUBTLE) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (whatsGood.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("What's Good", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                whatsGood.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
            }

            if (needsAttention.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PriorityHigh, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Needs Attention", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                }
                needsAttention.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
            }
            
            if (adjustments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Recommendations", style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                adjustments.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun StageRow(stage: ExecutionStage) {
    var expanded by remember { mutableStateOf(false) }
    AppCard(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        variant = if (stage.isComplete) AppCardVariant.SUBTLE else AppCardVariant.STANDARD
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (stage.isComplete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    null,
                    tint = if (stage.isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(stage.titleKey, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis, fontWeight = if (!stage.isComplete) FontWeight.Bold else FontWeight.Normal)
                Spacer(modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp, start = 36.dp)) {
                    Text(stage.descriptionKey, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    stage.actions.forEach { action ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (action.isDone) Icons.Default.Done else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(action.labelKey, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteRow(note: ExecutionNote) {
    AppCard(modifier = Modifier.fillMaxWidth(), variant = AppCardVariant.SUBTLE) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(note.text, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${note.scope} - ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(note.createdAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun FinancialOverviewCard(fin: GenerationFinancials) {
    AppCard(variant = AppCardVariant.SUBTLE) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                stringResource(UiR.string.breeding_generation_financials, fin.generation),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(UiR.string.breeding_actual_cost), style = MaterialTheme.typography.labelSmall)
                    Text("%.2f".format(fin.totalCostGross), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(UiR.string.breeding_actual_revenue), style = MaterialTheme.typography.labelSmall)
                    Text("%.2f".format(fin.totalRevenueGross), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
            }
        }
    }
}

@Composable
fun ExecutionDiagnosticsCard(diag: ExecutionDiagnostics) {
    AppCard(variant = AppCardVariant.STANDARD) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Engine Diagnostics (DEBUG)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("State Hash", style = MaterialTheme.typography.labelSmall)
                Text(diag.stateHash.toString(), style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recompute Time", style = MaterialTheme.typography.labelSmall)
                Text("${diag.lastRecomputeMs}ms", style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Last Persist", style = MaterialTheme.typography.labelSmall)
                val format = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                Text(if (diag.lastPersistAt == 0L) "Never" else format.format(java.util.Date(diag.lastPersistAt)), style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sample Size", style = MaterialTheme.typography.labelSmall)
                Text(diag.telemetrySampleSize.toString(), style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Link Reasons:", style = MaterialTheme.typography.labelSmall)
            diag.linkReasons.forEach { (reason, count) ->
                Text("• ${reason.name}: $count", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Evidence Quality:", style = MaterialTheme.typography.labelSmall)
            Text("• Coverage: ${diag.evidenceQuality.coverageScore}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
            val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            Text("• Freshness: ${if (diag.evidenceQuality.freshness == 0L) "N/A" else dateFormat.format(java.util.Date(diag.evidenceQuality.freshness))}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
            Text("• Link Conf: ${diag.evidenceQuality.linkConfidence}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
