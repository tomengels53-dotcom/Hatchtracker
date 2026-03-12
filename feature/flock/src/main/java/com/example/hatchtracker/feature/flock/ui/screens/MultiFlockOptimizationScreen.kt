package com.example.hatchtracker.feature.flock.ui.screens

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hatchtracker.domain.breeding.BreedingGoalType
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.feature.flock.ui.viewmodels.MultiFlockOptimizationViewModel
import com.example.hatchtracker.feature.flock.ui.viewmodels.OptimizationStep
import com.example.hatchtracker.common.util.RecommendedPair
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.core.ui.components.InlineDisclaimer
import com.example.hatchtracker.core.ui.components.AskHatchyCard
import com.example.hatchtracker.domain.model.UserProfile
import com.example.hatchtracker.core.ui.R as UiR
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MultiFlockOptimizationScreen(
    onBack: () -> Unit,
    profile: UserProfile? = null,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: MultiFlockOptimizationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val tier = if (profile?.subscriptionActive == true) {
        profile.subscriptionTier.name.let { SubscriptionTier.valueOf(it) }
    } else {
        SubscriptionTier.FREE
    }
    val isAdmin = profile?.isSystemAdmin == true || profile?.isDeveloper == true
    val canAccessBreeding = FeatureAccessPolicy.canAccess(FeatureKey.BREEDING, tier, isAdmin).allowed

    if (!canAccessBreeding) {
        BreedingLockedScreen(
            onBack = onBack,
            onViewPlans = onNavigateToPaywall
        )
        return
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.breeding_optimizer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                StepIndicator(step = 1, current = uiState.activeSteps.ordinal + 1, label = stringResource(UiR.string.breeding_step_flocks))
                Spacer(modifier = Modifier.width(32.dp))
                StepIndicator(step = 2, current = uiState.activeSteps.ordinal + 1, label = stringResource(UiR.string.breeding_step_goals))
                Spacer(modifier = Modifier.width(32.dp))
                StepIndicator(step = 3, current = uiState.activeSteps.ordinal + 1, label = stringResource(UiR.string.breeding_step_results))
            }

            AnimatedContent(targetState = uiState.activeSteps) { step ->
                when (step) {
                    OptimizationStep.SELECT_FLOCKS -> FlockSelectionStep(
                        flocks = uiState.availableFlocks,
                        selectedIds = uiState.selectedFlockIds,
                        onToggle = { viewModel.toggleFlockSelection(it) },
                        onNext = { viewModel.nextStep() }
                    )
                    OptimizationStep.CONFIGURE_WEIGHTS -> WeightsConfigurationStep(
                        selectedGoals = uiState.selectedGoals,
                        onToggleGoal = { viewModel.toggleGoal(it) },
                        onRun = { viewModel.nextStep() }
                    )
                    OptimizationStep.RESULTS -> ResultsView(
                        results = uiState.results,
                        isCalculating = uiState.isCalculating,
                        onReset = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

@Composable
fun BreedingLockedScreen(onBack: () -> Unit, onViewPlans: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(UiR.string.breeding_pro_feature), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(UiR.string.breeding_pro_required_multiflock), style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onViewPlans) {
            Text(stringResource(UiR.string.breeding_view_plans))
        }
        TextButton(onClick = onBack) {
            Text(stringResource(UiR.string.breeding_maybe_later))
        }
    }
}

@Composable
fun StepIndicator(step: Int, current: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (current >= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraLarge
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                color = if (current >= step) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun FlockSelectionStep(
    flocks: List<Flock>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(UiR.string.breeding_select_active_flocks), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(UiR.string.breeding_choose_flocks_optimize), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(flocks) { flock ->
                val isSelected = selectedIds.contains(flock.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onToggle(flock.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(flock.name, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                            Text(flock.species.name, style = MaterialTheme.typography.bodySmall)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        
        Button(
            onClick = onNext,
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(UiR.string.breeding_next_configure_goals))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeightsConfigurationStep(
    selectedGoals: Set<BreedingGoalType>,
    onToggleGoal: (BreedingGoalType) -> Unit,
    onRun: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(UiR.string.breeding_optimization_goals), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(UiR.string.breeding_objectives_cycle), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(stringResource(UiR.string.breeding_priority_goals), style = MaterialTheme.typography.titleMedium)
        FlowRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BreedingGoalType.values().forEach { goal ->
                FilterChip(
                    selected = selectedGoals.contains(goal),
                    onClick = { onToggleGoal(goal) },
                    label = { Text(goal.name.replace("_", " ")) },
                    leadingIcon = if (selectedGoals.contains(goal)) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onRun,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(UiR.string.breeding_run_optimization_engine))
        }
    }
}

@Composable
fun ResultsView(
    results: List<RecommendedPair>,
    isCalculating: Boolean,
    onReset: () -> Unit
) {
    if (isCalculating) {
         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 CircularProgressIndicator()
                 Spacer(modifier = Modifier.height(16.dp))
                 Text(stringResource(UiR.string.breeding_analyzing_genetics))
             }
         }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(UiR.string.breeding_recommended_pairs), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(UiR.string.breeding_results_found, results.size), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            InlineDisclaimer(
                title = stringResource(UiR.string.breeding_ai_generated_prediction),
                description = stringResource(UiR.string.breeding_prediction_disclaimer),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results) { pair ->
                    RecommendationCard(pair)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(UiR.string.breeding_start_over))
            }
        }
    }
}

@Composable
fun RecommendationCard(pair: RecommendedPair) {
    val summary = remember(pair) {
        pair.rationale.ifBlank { pair.predictedTraits.joinToString(", ").ifBlank { "No rationale available." } }
    }

    Card(
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${pair.male.breed} \u2642  x  ${pair.female.breed} \u2640", 
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        stringResource(UiR.string.breeding_points_short, pair.totalScore.toInt()),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            AskHatchyCard(
                summary = summary,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (pair.diversityScore > 0.8) {
                Text(
                    stringResource(UiR.string.breeding_high_genetic_diversity),
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

