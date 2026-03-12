package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.domain.breeding.MarketType
import kotlin.math.roundToInt

@Composable
fun PricingSettingsDialog(
    currentMargin: Double,
    currentMarket: MarketType,
    currentVatMode: com.example.hatchtracker.domain.finance.VatMode = com.example.hatchtracker.domain.finance.VatMode.DISABLED,
    currentVatRate: Double? = null,
    onMarginChange: (Double) -> Unit,
    onMarketChange: (MarketType) -> Unit,
    onVatModeChange: (com.example.hatchtracker.domain.finance.VatMode) -> Unit,
    onVatRateChange: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.pricing_settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.desired_profit_margin), style = MaterialTheme.typography.labelMedium)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = currentMargin.toFloat(),
                        onValueChange = { onMarginChange(it.toDouble()) },
                        valueRange = 0f..200f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${currentMargin.roundToInt()}%",
                        modifier = Modifier.width(48.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.target_market), style = MaterialTheme.typography.labelMedium)
                
                MarketType.values().forEach { market ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = currentMarket == market,
                            onClick = { onMarketChange(market) }
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(market.displayNameRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (market.premiumMultiplier != 1.0) {
                            Text(
                                text = "x${market.premiumMultiplier}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(stringResource(com.example.hatchtracker.core.ui.R.string.vat_configuration_title), style = MaterialTheme.typography.labelMedium)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(com.example.hatchtracker.core.ui.R.string.vat_mode_label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(currentVatMode.name)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            com.example.hatchtracker.domain.finance.VatMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.name) },
                                    onClick = {
                                        onVatModeChange(mode)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (currentVatMode != com.example.hatchtracker.domain.finance.VatMode.DISABLED) {
                    OutlinedTextField(
                        value = ((currentVatRate ?: 0.0) * 100).toString(),
                        onValueChange = { 
                            val rate = it.toDoubleOrNull()?.div(100.0)
                            onVatRateChange(rate)
                        },
                        label = { Text(stringResource(com.example.hatchtracker.core.ui.R.string.default_vat_rate_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.apply)) }
        }
    )
}


