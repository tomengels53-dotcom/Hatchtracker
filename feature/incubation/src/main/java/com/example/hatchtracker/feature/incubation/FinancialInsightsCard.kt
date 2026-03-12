package com.example.hatchtracker.feature.incubation

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.common.FinancialInsights
import com.example.hatchtracker.common.format.LocaleFormatService
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R as UiR

@Composable
fun FinancialInsightsCard(
    insights: FinancialInsights?,
    isPro: Boolean,
    currencyCode: String,
    localeFormatService: LocaleFormatService,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(UiR.string.financial_insights_title),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                )
                if (isPro) {
                    Spacer(modifier = Modifier.weight(1f))
                    SuggestionChip(
                        onClick = { },
                        label = { Text(stringResource(UiR.string.pro_label_chip), style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (insights != null) {
                InsightRow(
                    label = stringResource(UiR.string.cost_per_egg_label),
                    value = localeFormatService.formatCurrency((insights.costPerEgg * 100).toLong(), currencyCode),
                    icon = Icons.Default.Star
                )

                HorizontalDivider()

                if (isPro) {
                    ProInsightSection(insights, currencyCode, localeFormatService)
                } else {
                    GatedInsightSection(currencyCode, onUnlockClick)
                }
            } else {
                Text(
                    text = stringResource(UiR.string.no_cost_data_insights_msg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProInsightSection(insights: FinancialInsights, currencyCode: String, localeFormatService: LocaleFormatService) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InsightRow(
            label = stringResource(UiR.string.suggested_sale_price_label),
            value = localeFormatService.formatCurrency((insights.suggestedPricePerChick * 100).toLong(), currencyCode),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            subtext = stringResource(UiR.string.quality_bonus_format, insights.qualityBonus.toInt())
        )

        InsightRow(
            label = stringResource(UiR.string.cost_per_hatch_label),
            value = localeFormatService.formatCurrency((insights.costPerHatch * 100).toLong(), currencyCode),
            icon = Icons.Default.Star
        )
        
        InsightRow(
            label = stringResource(UiR.string.projected_profit_label),
            value = localeFormatService.formatCurrency((insights.projectedProfit * 100).toLong(), currencyCode),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            valueColor = if (insights.projectedProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun GatedInsightSection(currencyCode: String, onUnlockClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
             Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(UiR.string.suggested_sale_price_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = "$currencyCode --.--",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
             }
             Icon(
                 imageVector = Icons.Default.Lock,
                 contentDescription = stringResource(UiR.string.locked_label),
                 tint = MaterialTheme.colorScheme.primary,
                 modifier = Modifier.size(32.dp)
             )
        }
        
        Button(
            onClick = onUnlockClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(UiR.string.unlock_price_suggestions_action))
        }
    }
}

@Composable
private fun InsightRow(
    label: String,
    value: String,
    icon: ImageVector,
    subtext: String? = null,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (subtext != null) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
            color = valueColor
        )
    }
}
