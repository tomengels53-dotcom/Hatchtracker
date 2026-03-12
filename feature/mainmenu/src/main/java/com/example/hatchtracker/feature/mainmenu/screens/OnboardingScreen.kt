package com.example.hatchtracker.feature.mainmenu.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.navigation.NavRoute
import com.example.hatchtracker.feature.mainmenu.R
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (String) -> Unit, // Returns the route to navigate to
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedStartPoint by remember { mutableStateOf<StartPoint?>(null) }
    var selectedGoal by remember { mutableStateOf<TrackingGoal?>(null) }
    var showProExplanation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.onboarding_welcome_title)) },
                actions = {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(UiR.string.action_skip))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hatchy Guide
            HatchyGuideBanner(
                message = when (step) {
                    1 -> "Hi! I'm Hatchy. Let's get you set up. Where are we starting today?"
                    2 -> "Great! Now, what's our main goal for this batch?"
                    else -> "All set! Let's get to work."
                }
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "OnboardingStep"
            ) { targetStep ->
                when (targetStep) {
                    1 -> StepStartPoint(
                        selected = selectedStartPoint,
                        onSelected = {
                            selectedStartPoint = it
                            step = 2
                        }
                    )
                    2 -> StepTrackingGoal(
                        selected = selectedGoal,
                        onSelected = { goal ->
                            if (goal.isPro) {
                                selectedGoal = goal
                                showProExplanation = true
                            } else {
                                navigateToNext(selectedStartPoint, goal, onComplete)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showProExplanation) {
        AlertDialog(
            onDismissRequest = { showProExplanation = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text(stringResource(UiR.string.pro_feature_title)) },
            text = {
                Text(stringResource(UiR.string.pro_feature_onboarding_msg))
            },
            confirmButton = {
                Button(onClick = {
                    showProExplanation = false
                    navigateToNext(selectedStartPoint, selectedGoal, onComplete)
                }) {
                    Text(stringResource(UiR.string.action_continue_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProExplanation = false
                    selectedGoal = TrackingGoal.MILESTONES
                }) {
                    Text(stringResource(UiR.string.action_try_free_alternative))
                }
            }
        )
    }
}

@Composable
fun HatchyGuideBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = UiR.drawable.hatchy_1),
            contentDescription = "Hatchy",
            modifier = Modifier.size(64.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun StepStartPoint(
    selected: StartPoint?,
    onSelected: (StartPoint) -> Unit
) {
    Column(
        modifier = Modifier
            .selectableGroup()
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StartPoint.values().forEach { point ->
            OnboardingOptionCard(
                title = point.title,
                description = point.description,
                iconRes = point.iconRes,
                isSelected = selected == point,
                onClick = { onSelected(point) }
            )
        }
    }
}

@Composable
fun StepTrackingGoal(
    selected: TrackingGoal?,
    onSelected: (TrackingGoal) -> Unit
) {
    Column(
        modifier = Modifier
            .selectableGroup()
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TrackingGoal.values().forEach { goal ->
            OnboardingOptionCard(
                title = goal.title,
                description = goal.description,
                iconRes = goal.iconRes,
                isSelected = selected == goal,
                onClick = { onSelected(goal) },
                isPro = goal.isPro
            )
        }
    }
}

@Composable
fun OnboardingOptionCard(
    title: String,
    description: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    isPro: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    if (isPro) {
                        Spacer(Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(UiR.string.pro_label), style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

enum class StartPoint(val title: String, val description: String, val iconRes: Int) {
    EGGS("I have eggs to incubate", "Start from the very beginning.", R.drawable.hatch),
    CHICKS("I have chicks", "They're already out and about.", R.drawable.chick),
    ADULTS("I have adult birds", "Managing an existing flock.", R.drawable.hen)
}

enum class TrackingGoal(val title: String, val description: String, val iconRes: Int, val isPro: Boolean = false) {
    FINANCE("Costs + Revenue", "Keep an eye on the bottom line.", UiR.drawable.financial_profit),
    MILESTONES("Hatch Milestones", "Don't miss a beat (or a peep).", R.drawable.hatch),
    BREEDING("Breed Tracking", "Genetic history and insights.", R.drawable.cock, isPro = true)
}

private fun navigateToNext(
    startPoint: StartPoint?,
    goal: TrackingGoal?,
    onComplete: (String) -> Unit
) {
    val route = when (startPoint) {
        StartPoint.EGGS -> NavRoute.AddIncubation.createRoute()
        StartPoint.CHICKS -> NavRoute.Nursery.route // Nursery is the best hub for chicks
        StartPoint.ADULTS -> NavRoute.AddFlock.createRoute()
        null -> NavRoute.MainMenu.createRoute("home")
    }
    onComplete(route)
}
