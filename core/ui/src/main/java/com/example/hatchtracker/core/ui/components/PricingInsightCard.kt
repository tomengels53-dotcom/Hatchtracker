package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.domain.pricing.*
import com.example.hatchtracker.domain.breeding.MarketType 
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PricingInsightCard(
    suggestion: PricingSuggestion, 
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val isLossMaking = suggestion.suggestedUnitPrice <= suggestion.unitCost
    val isUnderpriced = suggestion.assumptionCodes.contains(AssumptionCode.LOWER_THAN_HISTORICAL_AVG)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLossMaking) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.finance_pricing_title), style = MaterialTheme.typography.labelMedium)
                if (isUnderpriced) {
                    Text(stringResource(R.string.finance_pricing_label_underpriced), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.finance_pricing_label_seek, formatCurrency(suggestion.suggestedUnitPrice, currencyCode)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Break-even Display (Transparency)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.finance_pricing_label_breakeven, formatCurrency(suggestion.unitCost, currencyCode)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Explanation Builder
            val explanation = buildString {
                append(stringResource(R.string.finance_pricing_msg_margin, suggestion.marginPercent))
                append(" ")
                if (suggestion.marketType != MarketType.LOCAL) {
                     append(stringResource(R.string.finance_pricing_msg_market_adjustment, stringResource(suggestion.marketType.displayNameRes)))
                     append(" ")
                }
                if (suggestion.assumptionCodes.contains(AssumptionCode.USED_DEFAULT_VALUES)) {
                   append(stringResource(R.string.finance_pricing_msg_default_estimates))
                }
            }
            
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )

            // Confidence Level
            Text(
                text = stringResource(R.string.finance_pricing_label_confidence, suggestion.confidence.name),
                style = MaterialTheme.typography.labelSmall,
                color = when(suggestion.confidence) {
                    ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.primary
                    ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.secondary
                    ConfidenceLevel.LOW -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatCurrency(amount: Double, currencyCode: String): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = java.util.Currency.getInstance(currencyCode)
        format.format(amount)
    } catch (e: Exception) {
        // Fallback for invalid currency codes
        "${currencyCode} ${String.format("%.2f", amount)}"
    }
}
