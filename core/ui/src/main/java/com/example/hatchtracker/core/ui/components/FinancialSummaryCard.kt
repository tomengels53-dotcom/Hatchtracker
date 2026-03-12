package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.common.util.CurrencyUtils
import com.example.hatchtracker.data.models.FinancialSummary

@Composable
fun FinancialSummaryCard(
    summary: FinancialSummary?,
    modifier: Modifier = Modifier,
    currencyCode: String? = null,
    tier: com.example.hatchtracker.data.models.SubscriptionTier = com.example.hatchtracker.data.models.SubscriptionTier.FREE,
    isAdmin: Boolean = false,
    isDeveloper: Boolean = false,
    onAddCostClick: () -> Unit = {},
    onAddRevenueClick: () -> Unit = {},
    onDeepdiveClick: () -> Unit = {}
) {
    val canDeepdive = tier == com.example.hatchtracker.data.models.SubscriptionTier.EXPERT || 
                     tier == com.example.hatchtracker.data.models.SubscriptionTier.PRO ||
                     isAdmin || isDeveloper

    AppCard(
        modifier = modifier.then(
            if (canDeepdive) Modifier.clickable { onDeepdiveClick() } else Modifier
        ),
        variant = AppCardVariant.STANDARD
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.financial_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface 
                )
                
                if (canDeepdive) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.financial_tap_for_details),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.costs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // Subdued label
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(summary?.totalCosts ?: 0.0, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error // Warm Red
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.revenue),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(summary?.totalRevenue ?: 0.0, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary // Warm Green
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.profit_loss),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    val profit = summary?.profit ?: 0.0
                    Text(
                        text = CurrencyUtils.formatCurrency(profit, currencyCode),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (profit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                val unitMetricValue = when {
                    summary?.costPerEgg != null && summary.costPerEgg!! > 0 -> summary.costPerEgg to androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.per_egg)
                    summary?.costPerChick != null && summary.costPerChick!! > 0 -> summary.costPerChick to androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.per_chick)
                    summary?.costPerAdult != null && summary.costPerAdult!! > 0 -> summary.costPerAdult to androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.per_adult)
                    else -> null
                }

                if (unitMetricValue != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = unitMetricValue.second, 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(unitMetricValue.first!!, currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddCostClick,
                    modifier = Modifier.weight(1f),
                    shape = AppSurfaceSpec.ShapeInput
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.add_cost))
                }
                Button(
                    onClick = onAddRevenueClick,
                    modifier = Modifier.weight(1f),
                    shape = AppSurfaceSpec.ShapeInput
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.add_revenue))
                }
            }
        }
    }
}
