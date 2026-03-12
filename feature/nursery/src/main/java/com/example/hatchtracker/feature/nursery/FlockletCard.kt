package com.example.hatchtracker.feature.nursery

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.Image
import com.example.hatchtracker.core.ui.composeutil.premiumClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.Flocklet
import com.example.hatchtracker.core.common.NurseryConfig
import kotlin.math.roundToInt
import com.example.hatchtracker.data.models.FinancialSummary
import java.text.NumberFormat
import java.util.Locale
import com.example.hatchtracker.domain.breeding.CurrencyUtils
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.feature.nursery.R as NurR

@Deprecated("Replaced by CompactRowItem and StandardActionSheet for performance and scalability.")
@Composable
fun FlockletCard(
    flocklet: Flocklet,
    financialSummary: FinancialSummary?,
    advice: String?,
    onEditClick: () -> Unit,
    onMoveClick: () -> Unit,
    onReadyClick: () -> Unit,
    onAddCostClick: () -> Unit,
    onAddRevenueClick: () -> Unit,
    onSellClick: () -> Unit,
    onRecordLossValid: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onDeepdiveClick: () -> Unit = {},
    showFinancials: Boolean = true,
    canDeepdive: Boolean = false,
    currencyCode: String = "USD"
) {
    val rule = NurseryConfig.getRuleForSpecies(flocklet.species)
    val progress = (flocklet.ageInDays.toFloat() / rule.minAgeForFlock.toFloat()).coerceIn(0f, 1f)
    val isReady = flocklet.readyForFlock

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        variant = AppCardVariant.STANDARD,
        colors = if (isReady) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)) 
        else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Species & Status
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${flocklet.species} - ${flocklet.breeds.joinToString(", ")}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                    Text(
                        text = stringResource(NurR.string.current_stage_nursery),
                        color = MaterialTheme.colorScheme.primary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                    Text(
                        text = stringResource(
                            NurR.string.chicks_days_to_graduation,
                            flocklet.chickCount,
                            flocklet.ageInDays,
                            rule.minAgeForFlock
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Icon - Using R from feature:flock or core:ui via dependency
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.hatchtracker.core.ui.R.drawable.chick),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).padding(end = 8.dp)
                )
                
                // Status Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isReady) {
                        SuggestionChip(
                            onClick = onReadyClick,
                            label = { Text(stringResource(NurR.string.ready_label)) },
                            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    } else if (flocklet.healthStatus != "Healthy") {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(NurR.string.health_issue_desc),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(NurR.string.delete_action),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row: Temperature
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Temperature Section
                Icon(
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(stringResource(NurR.string.recommended_brooder_temp), style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = String.format("%.1f\u00B0C", flocklet.targetTemp),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Health Indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(NurR.string.health_label), style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = flocklet.healthStatus,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                        color = when(flocklet.healthStatus) {
                            "Healthy" -> MaterialTheme.colorScheme.primary
                            "Issues" -> MaterialTheme.colorScheme.tertiary
                            "Critical" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Hatchy Advice Section
            advice?.let { tip ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.hatchtracker.core.ui.R.drawable.hatchy_1),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(NurR.string.hatchy_tip_title), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tip, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Readiness Progress
            Column {
                val daysRemaining = (rule.minAgeForFlock - flocklet.ageInDays).coerceAtLeast(0)
                val graduationText = if (isReady) {
                    stringResource(NurR.string.ready_to_graduate)
                } else {
                    stringResource(NurR.string.days_left_format, daysRemaining)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(NurR.string.time_to_graduation), style = MaterialTheme.typography.labelSmall)
                    Text(graduationText, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            if (showFinancials) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (canDeepdive) Modifier.premiumClickable { onDeepdiveClick() } else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(NurR.string.financials_label), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                            val profit = financialSummary?.profit ?: 0.0
                            Text(
                                text = stringResource(NurR.string.profit_label_format, CurrencyUtils.formatCurrency(profit, currencyCode)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (profit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        Row {
                            TextButton(onClick = { /* Stop propagation if needed, but these are small tap targets */ onAddCostClick() }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text(stringResource(NurR.string.minus_currency), color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = onAddRevenueClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text(stringResource(NurR.string.plus_currency), color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = onSellClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text(stringResource(NurR.string.sell_action_caps), color = MaterialTheme.colorScheme.primary, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                            }
                            TextButton(onClick = onRecordLossValid, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text(stringResource(NurR.string.loss_action_caps), color = MaterialTheme.colorScheme.error, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                            }
                        }
                    }
                    if (canDeepdive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(NurR.string.tap_for_details),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(NurR.string.update_status_notes))
                }

                if (isReady) {
                    Button(
                        onClick = onMoveClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(NurR.string.move_to_flock))
                    }
                }
            }
        }
    }
}








