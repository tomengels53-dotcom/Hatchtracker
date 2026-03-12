package com.example.hatchtracker.feature.incubation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.common.IncubationHubSummary
import com.example.hatchtracker.common.format.LocaleFormatService
import com.example.hatchtracker.domain.breeding.IncubationUtils
import com.example.hatchtracker.core.ui.R as UiR

@Composable
fun IncubationHubSummaryCard(
    summary: IncubationHubSummary,
    currencyCode: String,
    localeFormatService: LocaleFormatService,
    modifier: Modifier = Modifier
) {
    if (summary.batchCount == 0) return

    AppCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(UiR.string.hatchery_performance_summary_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(UiR.string.last_n_batches_format, summary.batchCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStatItem(
                    label = stringResource(UiR.string.avg_fertility_label),
                    value = IncubationUtils.formatPercentage(summary.averageFertility),
                    modifier = Modifier.weight(1f)
                )
                SummaryStatItem(
                    label = stringResource(UiR.string.avg_hatchability_label),
                    value = IncubationUtils.formatPercentage(summary.averageHatchability),
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                if (summary.hasFinancialData) {
                    SummaryStatItem(
                        label = stringResource(UiR.string.avg_hatch_cost_label),
                        value = localeFormatService.formatCurrency((summary.averageCostPerHatch * 100).toLong(), currencyCode),
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
