package com.example.hatchtracker.feature.incubation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.R as UiR

import com.example.hatchtracker.core.ui.theme.bodyEmphasis
import com.example.hatchtracker.domain.breeding.CurrencyUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HatchPlannerScreen(
    onBackClick: () -> Unit,
    viewModel: HatchPlannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hatch_planner_title)) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(stringResource(UiR.string.back_action))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(
                                    R.string.forecasting_intro_format,
                                    uiState.historyCount
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (uiState.historyCount == 0) {
                        Text(
                            text = stringResource(R.string.not_enough_data_msg),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    SectionHeader(stringResource(R.string.assumptions_header))

                    InputSlider(
                        label = stringResource(R.string.planned_egg_count_label),
                        value = uiState.plannedEggs.toFloat(),
                        onValueChange = { viewModel.updatePlannedEggs(it.roundToInt()) },
                        valueRange = 1f..1000f,
                        steps = 100,
                        valueDisplay = stringResource(
                            R.string.planned_eggs_display_format,
                            uiState.plannedEggs
                        )
                    )

                    InputSlider(
                        label = stringResource(R.string.expected_hatch_rate_label),
                        value = uiState.assumedHatchRate,
                        onValueChange = { viewModel.updateAssumedHatchRate(it) },
                        valueRange = 0f..100f,
                        steps = 20,
                        valueDisplay = stringResource(
                            R.string.hatch_rate_percent_format,
                            uiState.assumedHatchRate.roundToInt()
                        )
                    )

                    HorizontalDivider()
                    SectionHeader(stringResource(R.string.projections_header))

                    uiState.forecast?.let { forecast ->
                        ForecastMetricCard(
                            title = stringResource(R.string.estimated_total_cost_label),
                            value = CurrencyUtils.formatCurrency(forecast.totalCost),
                            subtitle = stringResource(R.string.total_investment_subtitle)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ForecastMetricCard(
                                title = stringResource(R.string.per_egg_label),
                                value = CurrencyUtils.formatCurrency(forecast.costPerEgg),
                                modifier = Modifier.weight(1f)
                            )
                            ForecastMetricCard(
                                title = stringResource(R.string.per_chick_label),
                                value = CurrencyUtils.formatCurrency(forecast.costPerChick),
                                modifier = Modifier.weight(1f),
                                highlight = true
                            )
                        }

                        if (forecast.confidenceInterval > 0) {
                            val confidence = if (forecast.confidenceInterval < 0.5) {
                                stringResource(R.string.confidence_high)
                            } else {
                                stringResource(R.string.confidence_medium)
                            }
                            Text(
                                text = stringResource(
                                    R.string.confidence_level_format,
                                    confidence,
                                    CurrencyUtils.formatCurrency(forecast.confidenceInterval)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(R.string.planning_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyEmphasis,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun InputSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueDisplay: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.bodyEmphasis,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
fun ForecastMetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyEmphasis,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
