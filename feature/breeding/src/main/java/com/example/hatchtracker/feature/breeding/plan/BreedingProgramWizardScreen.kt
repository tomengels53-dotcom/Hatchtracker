package com.example.hatchtracker.feature.breeding.plan

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.data.models.GenEstimate
import com.example.hatchtracker.data.models.GoalSpec
import com.example.hatchtracker.data.models.StartingSituation
import com.example.hatchtracker.data.models.StrategyConfig
import com.example.hatchtracker.data.models.StrategyMode
import com.example.hatchtracker.data.models.TraitDomain
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.domain.breeding.plan.ProgramMode
import com.example.hatchtracker.domain.breeding.*
import com.example.hatchtracker.feature.breeding.ui.components.BreedingIntelligenceCard
import com.example.hatchtracker.model.breeding.GeneticInsightUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingProgramWizardScreen(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: BreedingProgramWizardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { _ ->
            onBack() 
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_title)) },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.currentStep == GoalWizardStep.SPECIES) onBack() else viewModel.backStep() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Progress Indicator
            LinearProgressIndicator(
                progress = { (uiState.currentStep.ordinal + 1).toFloat() / GoalWizardStep.entries.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.currentStep) {
                GoalWizardStep.SPECIES -> SpeciesSelectionStep(onSpeciesSelected = { viewModel.setSpecies(it) })
                GoalWizardStep.SITUATION -> SituationSelectionStep(onSituationSelected = { viewModel.setStartingSituation(it) })
                GoalWizardStep.FLOCKS -> SourceSelectionStep(
                    flocks = uiState.availableFlocks,
                    selectedIds = uiState.selectedFlockIds,
                    onToggle = { viewModel.toggleFlock(it) },
                    onProceed = { viewModel.proceedToTraits() }
                )
                GoalWizardStep.TRAIT_DOMAINS -> TraitDomainStep(
                    selectedDomains = uiState.strategyConfig.goalSpecs.map { it.domain }.toSet(),
                    onToggleDomain = { viewModel.toggleTraitDomain(it) },
                    onProceed = { viewModel.proceedToTraitSpecs() }
                )
                GoalWizardStep.TRAIT_SPECS -> TraitSpecsStep(
                    refinementState = uiState.refinementState,
                    domains = uiState.strategyConfig.goalSpecs.map { it.domain }.toSet(),
                    onSpecsConfirmed = { viewModel.setGoalSpecs(it) },
                    onRetry = { viewModel.retryRefinement() },
                    onGoBack = { viewModel.backStep() }
                )
                GoalWizardStep.STRATEGY_MODE -> StrategyModeStep(
                    onModeSelected = { viewModel.setStrategyMode(it) }
                )
                GoalWizardStep.ANALYSIS -> AnalysisStep(
                    estimate = uiState.genEstimate,
                    geneticInsight = uiState.geneticInsight,
                    onProceed = { viewModel.proceedToDraft() }
                )
                GoalWizardStep.DRAFT -> DraftReviewStep(
                    draft = uiState.draft,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    isPro = uiState.isPro,
                    onUpgrade = onNavigateToPaywall,
                    onConfirm = { viewModel.confirmDraft() }
                )
            }
        }
    }
}

@Composable
fun SpeciesSelectionStep(onSpeciesSelected: (Species) -> Unit) {
    Column {
        Text(
            androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_step_species),
            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Species.entries) { species ->
                Card(
                    onClick = { onSpeciesSelected(species) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = species.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SituationSelectionStep(onSituationSelected: (StartingSituation) -> Unit) {
    Column {
        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_step_situation), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        SituationCard(
            title = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_situation_a_title),
            desc = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_situation_a_desc),
            icon = Icons.Default.Home,
            onClick = { onSituationSelected(StartingSituation.COMBINE_FLOCKS) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SituationCard(
            title = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_situation_b_title),
            desc = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_situation_b_desc),
            icon = Icons.Default.Add,
            onClick = { onSituationSelected(StartingSituation.IMPROVE_FLOCK) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SituationCard(
            title = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_situation_c_title),
            desc = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_situation_c_desc),
            icon = Icons.Default.Create,
            onClick = { onSituationSelected(StartingSituation.START_FROM_SCRATCH) }
        )
    }
}

@Composable
fun SituationCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun TraitDomainStep(
    selectedDomains: Set<TraitDomain>,
    onToggleDomain: (TraitDomain) -> Unit,
    onProceed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_step_domains), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(TraitDomain.entries) { domain ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = selectedDomains.contains(domain), onCheckedChange = { onToggleDomain(domain) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(domain.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() })
                }
            }
        }
        
        Button(
            onClick = onProceed,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDomains.isNotEmpty()
        ) {
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.action_next))
        }
    }
}

@Composable
fun TraitSpecsStep(
    refinementState: RefinementUiState,
    domains: Set<TraitDomain>,
    onSpecsConfirmed: (List<GoalSpec>) -> Unit,
    onRetry: () -> Unit,
    onGoBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_step_specs), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when (refinementState) {
            is RefinementUiState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is RefinementUiState.Empty -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    com.example.hatchtracker.core.ui.components.EmptyStatePanel(
                        title = "No Trait Domains Selected",
                        description = "Please go back and select at least one trait domain to refine targets.",
                        action = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onGoBack) {
                                    Text("Go Back")
                                }
                                Button(onClick = onRetry) {
                                    Text("Retry")
                                }
                            }
                        }
                    )
                }
            }
            is RefinementUiState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    com.example.hatchtracker.core.ui.components.EmptyStatePanel(
                        title = "Failed to Shape Traits",
                        description = refinementState.message,
                        action = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onGoBack) {
                                    Text("Go Back")
                                }
                                Button(onClick = onRetry) {
                                    Text("Retry")
                                }
                            }
                        }
                    )
                }
            }
            is RefinementUiState.Ready -> {
                val specs = refinementState.specs
                Text("Refining targets for: ${domains.joinToString { it.name.lowercase().replace("_", " ") }}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onSpecsConfirmed(specs) }, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.action_next))
                }
            }
        }
    }
}

@Composable
fun StrategyModeStep(onModeSelected: (StrategyMode) -> Unit) {
    Column {
        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_step_strategy_mode), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        SituationCard(
            title = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_mode_strict_title),
            desc = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_mode_strict_desc),
            icon = Icons.Default.Info,
            onClick = { onModeSelected(StrategyMode.STRICT_LINE_BREEDING) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SituationCard(
            title = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_mode_commercial_title),
            desc = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_mode_commercial_desc),
            icon = Icons.Default.ShoppingCart,
            onClick = { onModeSelected(StrategyMode.COMMERCIAL_PRODUCTION) }
        )
    }
}

@Composable
fun AnalysisStep(
    estimate: GenEstimate?,
    geneticInsight: GeneticInsightUiModel?,
    onProceed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_step_analysis), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        BreedingIntelligenceCard(uiModel = geneticInsight)
        
        Spacer(modifier = Modifier.height(16.dp))

        if (estimate != null) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_gen_estimate_label), style = MaterialTheme.typography.labelLarge)
                    Text("${estimate.minGenerations} - ${estimate.maxGenerations} generations", style = MaterialTheme.typography.headlineSmall)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_confidence_label)} ${estimate.confidence}", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_limiting_factors_label), style = MaterialTheme.typography.titleSmall)
            estimate.limitingFactors.forEach { factor ->
                Text("â€¢ $factor", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            CircularProgressIndicator()
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onProceed, modifier = Modifier.fillMaxWidth()) {
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_generate))
        }
    }
}

@Composable
fun DraftReviewStep(
    draft: com.example.hatchtracker.domain.breeding.plan.BreedingPlanDraft?,
    isLoading: Boolean,
    error: String?,
    isPro: Boolean,
    onUpgrade: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (error != null) {
        val errorText = when (error) {
            "PRO_FEATURE_REQUIRED" -> androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_error_pro_only)
            "NO_VIABLE_STRATEGY" -> androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_error_no_viable_plan, "")
            else -> androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_error_generate, error)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
            Text(errorText, color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (error == "PRO_FEATURE_REQUIRED") {
                Button(onClick = onUpgrade) { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_upgrade_action)) }
            }
        }
        return
    }
    if (draft == null) return

    Column {
        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_draft_title), style = MaterialTheme.typography.titleLarge)
        Text(draft.summaryRationale, style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            items(draft.steps) { step ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(step.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(step.instruction, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (!isPro) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_upgrade_pro), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_upgrade_pro_desc), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onUpgrade) { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_upgrade_action)) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = onConfirm, 
            modifier = Modifier.fillMaxWidth(),
            enabled = if (draft.planType == ProgramMode.FLOCK_BASED) isPro else true
        ) { 
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_activate)) 
        }
    }
}

@Composable
fun SourceSelectionStep(
    flocks: List<com.example.hatchtracker.data.models.Flock>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onProceed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.breeding_program_wizard_step_sources), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(flocks) { flock ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selectedIds.contains(flock.syncId), onCheckedChange = { onToggle(flock.syncId) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(flock.name)
                }
            }
        }
        Button(
            onClick = onProceed, 
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedIds.isNotEmpty()
        ) { 
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.action_next)) 
        }
    }
}
