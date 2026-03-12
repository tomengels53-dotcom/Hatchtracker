package com.example.hatchtracker.feature.breeding

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import com.example.hatchtracker.model.BreedingSafeguard
import com.example.hatchtracker.core.ui.composeutil.premiumClickable

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Sex
import com.example.hatchtracker.data.models.BreedingGoalType
import com.example.hatchtracker.data.models.BasicRecommendation
import com.example.hatchtracker.core.ui.catalog.DefaultCatalogData
import com.example.hatchtracker.data.models.StrategicRecommendation
import com.example.hatchtracker.core.common.asString
import com.example.hatchtracker.core.ui.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BreedingScreen(
    onBack: () -> Unit,
    onStartIncubation: (Long) -> Unit = {},
    canAccessBreeding: Boolean,
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToBreedSelection: (String) -> Unit = {},
    onNavigateToTraitObservation: (breedId: String, traitId: String) -> Unit = { _, _ -> },
    onNavigateToMultiFlockOptimization: () -> Unit = {},
    onNavigateToScenario: () -> Unit = {},
    selectedBreedFromResult: String? = null,
    onClearBreedResult: () -> Unit = {},
    viewModel: com.example.hatchtracker.feature.breeding.BreedingSelectionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    if (!canAccessBreeding) {
        BreedingLockedScreen(onBack = onBack, onViewPlans = onNavigateToPaywall)
        return
    }

    LaunchedEffect(selectedBreedFromResult) {
        selectedBreedFromResult?.let { breedName ->
            if (viewModel.uiState.value.isScenarioMode) {
                viewModel.handleBreedSelectionResult(breedName)
            }
            onClearBreedResult()
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val canStart by viewModel.canStartBreeding.collectAsState()

    if (uiState.confirmedRecordId != null) {
        BreedingSuccessView(
            onDone = onBack,
            onStartIncubation = { onStartIncubation(uiState.confirmedRecordId!!) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(R.string.breeding_title_selection)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(R.string.back_action))
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToMultiFlockOptimization) {
                        Icon(Icons.Default.AutoGraph, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.breeding_plan_builder))
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.selectedMale != null || uiState.selectedFemales.isNotEmpty()) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (uiState.selectedMale != null) androidx.compose.ui.res.stringResource(R.string.breeding_label_sire, uiState.selectedMale!!.breed)
                                else androidx.compose.ui.res.stringResource(R.string.breeding_label_no_sire),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_dams_selected, uiState.selectedFemales.size), style = MaterialTheme.typography.bodySmall)
                        }
                        
                        val isBlocked = uiState.safeguard is BreedingSafeguard.BlockingLethal
                        val needsAck = uiState.safeguard is BreedingSafeguard.WarningInbreeding && !uiState.hasAcknowledgedSafeguard

                        Button(
                            onClick = { if (!needsAck) viewModel.showConfirmDialog() },
                            enabled = canStart && !uiState.isProcessing && !isBlocked
                        ) {
                            if (uiState.isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text(androidx.compose.ui.res.stringResource(R.string.breeding_button_confirm))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current) + 16.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    end = paddingValues.calculateEndPadding(androidx.compose.ui.platform.LocalLayoutDirection.current) + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_goals), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BreedingGoalType.values().forEach { goalType ->
                            FilterChip(
                                selected = uiState.selectedGoals.contains(goalType),
                                onClick = { viewModel.toggleGoal(goalType) },
                                label = { Text(goalType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                leadingIcon = if (uiState.selectedGoals.contains(goalType)) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(24.dp)) }
                                } else null
                            )
                        }
                    }
                }

                if (uiState.isScenarioMode) {
                   // Scenario Mode legacy call removed for unified Plan Builder
                } else {
                    if (uiState.strategicRecommendation != null) {
                        item { StrategicOutcomeCard(recommendation = uiState.strategicRecommendation!!, onGoalClick = { onNavigateToMultiFlockOptimization() }) }
                    } else if (uiState.basicRecommendation != null) {
                        item { BasicOutcomeCard(uiState.basicRecommendation!!) }
                        item { ProUpsellCard() }
                    }

                    uiState.hatchyAdvice?.let { advice ->
                        item { Text(advice.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    }
    
                    uiState.breedingInsights.forEach { insight ->
                        item { 
                            WarningBanner(insight = insight)
                        }
                    }
    
                    uiState.flocks.forEach { flock ->
                        val flockBirds = uiState.birdsByFlock[flock.id] ?: emptyList()
                        if (flockBirds.isNotEmpty()) {
                            item { Text(flock.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                            items(flockBirds) { bird ->
                                BirdSelectionCard(
                                    bird = bird,
                                    isSelected = uiState.selectedMale?.id == bird.id || uiState.selectedFemales.any { it.id == bird.id },
                                    onClick = { viewModel.toggleSelection(bird) },
                                    onTraitClick = { traitId -> onNavigateToTraitObservation(bird.breed, traitId) }
                                )
                            }
                        }
                    }
                    
                    if (uiState.flocks.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_msg_no_flocks), style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = onNavigateToMultiFlockOptimization) {
                                        Text(androidx.compose.ui.res.stringResource(R.string.open_breeding_plan_builder))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (uiState.isConfirmDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            title = { Text(androidx.compose.ui.res.stringResource(R.string.breeding_dialog_title_confirm)) },
            text = {
                Column {
                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_dialog_msg_confirm))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_sire, uiState.selectedMale?.breed ?: ""), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_dams_selected, uiState.selectedFemales.size), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
            },
            confirmButton = { Button(onClick = { viewModel.confirmBreeding() }) { Text(androidx.compose.ui.res.stringResource(R.string.action_confirm)) } },
            dismissButton = { TextButton(onClick = { viewModel.dismissConfirmDialog() }) { Text(androidx.compose.ui.res.stringResource(R.string.action_cancel)) } }
        )
    }

    if (uiState.safeguard != BreedingSafeguard.None && !uiState.hasAcknowledgedSafeguard) {
        val safeguard = uiState.safeguard
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    if (safeguard is BreedingSafeguard.BlockingLethal) androidx.compose.ui.res.stringResource(R.string.breeding_dialog_title_blocked) else androidx.compose.ui.res.stringResource(R.string.breeding_dialog_title_warning),
                    color = if (safeguard is BreedingSafeguard.BlockingLethal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
            },
            text = {
                Column {
                    val copyRes = when (safeguard) {
                        is BreedingSafeguard.BlockingLethal -> R.string.breeding_safeguard_lethal_copy
                        is BreedingSafeguard.WarningInbreeding -> R.string.breeding_safeguard_inbreeding_copy
                        else -> R.string.error_unknown
                    }
                    Text(androidx.compose.ui.res.stringResource(copyRes))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_educational_disclaimer), style = MaterialTheme.typography.labelSmall, fontStyle = FontStyle.Italic)
                }
            },
            confirmButton = {
                if (safeguard is BreedingSafeguard.WarningInbreeding) {
                    Button(onClick = { viewModel.acknowledgeSafeguard() }) { Text(androidx.compose.ui.res.stringResource(R.string.breeding_button_acknowledge)) }
                } else {
                    TextButton(onClick = onBack) { Text(androidx.compose.ui.res.stringResource(R.string.breeding_button_cancel_pairing)) }
                }
            },
            dismissButton = { if (safeguard is BreedingSafeguard.WarningInbreeding) { TextButton(onClick = onBack) { Text(androidx.compose.ui.res.stringResource(R.string.action_cancel)) } } },
            icon = { Icon(if (safeguard is BreedingSafeguard.BlockingLethal) Icons.Default.Block else Icons.Default.Warning, contentDescription = null, tint = if (safeguard is BreedingSafeguard.BlockingLethal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary) }
        )
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(androidx.compose.ui.res.stringResource(R.string.dialog_title_error)) },
            text = { Text(uiState.error?.asString() ?: androidx.compose.ui.res.stringResource(R.string.error_unknown)) },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text(androidx.compose.ui.res.stringResource(R.string.action_ok)) } }
        )
    }
}

@Composable
fun BreedingSuccessView(onDone: () -> Unit, onStartIncubation: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(androidx.compose.ui.res.stringResource(R.string.breeding_success_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(androidx.compose.ui.res.stringResource(R.string.breeding_success_msg), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onStartIncubation, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(R.string.breeding_button_start_incubation))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(androidx.compose.ui.res.stringResource(R.string.breeding_button_back_dashboard)) }
        }
    }
}

@Composable
fun BasicOutcomeCard(recommendation: BasicRecommendation) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), border = CardDefaults.outlinedCardBorder()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_basic_compatibility), style = MaterialTheme.typography.titleMedium)
                }
                Surface(shape = MaterialTheme.shapes.small, color = if (recommendation.score >= 70) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFFFFC107)) {
                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_match_score, recommendation.score), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = androidx.compose.ui.graphics.Color.White, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(recommendation.basicSummary.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun StrategicOutcomeCard(recommendation: StrategicRecommendation, onGoalClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = androidx.compose.ui.res.stringResource(R.string.pro_label_chip), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_strategic_analysis), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {
                    Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_strategic_match_score, recommendation.score), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimary, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_expected_outcomes), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            recommendation.expectedOutcomes.forEach { outcome ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(outcome, style = MaterialTheme.typography.bodySmall)
                }
            }
            // Simplified for restoration
            Spacer(modifier = Modifier.height(12.dp))
            Text(androidx.compose.ui.res.stringResource(R.string.breeding_label_strategic_rationale), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
            Text(recommendation.strategicRationale.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProUpsellCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = androidx.compose.ui.res.stringResource(R.string.locked), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(androidx.compose.ui.res.stringResource(R.string.breeding_upsell_title), style = MaterialTheme.typography.titleSmall)
                Text(androidx.compose.ui.res.stringResource(R.string.breeding_upsell_msg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text(androidx.compose.ui.res.stringResource(R.string.breeding_button_upgrade_pro), style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
fun WarningBanner(
    insight: BreedingInsight,
    modifier: Modifier = Modifier
) {
    val containerColor = when (insight.severity) {
        InsightSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        InsightSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        InsightSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    }
    val contentColor = when (insight.severity) {
        InsightSeverity.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
        InsightSeverity.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        InsightSeverity.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val icon = when (insight.severity) {
        InsightSeverity.CRITICAL -> Icons.Default.Block
        InsightSeverity.WARNING -> Icons.Default.Warning
        InsightSeverity.INFO -> Icons.Default.Info
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = insight.title.asString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.body.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )
                insight.actionHint?.let { hint ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = hint.asString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BirdSelectionCard(bird: Bird, isSelected: Boolean, onClick: () -> Unit, onTraitClick: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().premiumClickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface), border = if (isSelected) null else CardDefaults.outlinedCardBorder()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val isLocked = bird.status != "active"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(bird.breed, style = MaterialTheme.typography.titleMedium, color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) { Text("F${bird.generation}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis) }
                    }
                    Text("${bird.sex} \u2022 ${bird.species.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isSelected) { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                FlowRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val traits = (bird.geneticProfile.fixedTraits + bird.geneticProfile.inferredTraits).take(3)
                    traits.forEach { trait -> SuggestionChip(onClick = { onTraitClick(trait) }, label = { Text(trait, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
