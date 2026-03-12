package com.example.hatchtracker.feature.incubation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.common.format.LocaleFormatService
import com.example.hatchtracker.domain.breeding.IncubationUtils
import com.example.hatchtracker.core.ui.R as UiR

@Composable
fun IncubationPerformanceCard(
    fertilityRate: Float,
    hatchability: Float,
    costPerHatch: Double?,
    currencyCode: String,
    localeFormatService: LocaleFormatService,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(UiR.string.performance_metrics_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            MetricRow(
                label = stringResource(UiR.string.fertility_rate_label),
                value = IncubationUtils.formatPercentage(fertilityRate),
                icon = Icons.Default.Science
            )
            
            MetricRow(
                label = stringResource(UiR.string.hatchability_label),
                value = IncubationUtils.formatPercentage(hatchability),
                icon = Icons.Default.Egg
            )
            
            if (costPerHatch != null && costPerHatch > 0) {
                MetricRow(
                    label = stringResource(UiR.string.cost_per_hatch_label),
                    value = localeFormatService.formatCurrency((costPerHatch * 100).toLong(), currencyCode),
                    icon = Icons.Default.Payments
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
