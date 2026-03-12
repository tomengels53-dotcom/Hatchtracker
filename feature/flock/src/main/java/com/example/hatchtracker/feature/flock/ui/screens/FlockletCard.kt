package com.example.hatchtracker.feature.flock.ui.screens

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.Image
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
import com.example.hatchtracker.common.util.NurseryConfig
import kotlin.math.roundToInt
import com.example.hatchtracker.data.models.FinancialSummary
import java.text.NumberFormat
import java.util.Locale
import com.example.hatchtracker.common.util.CurrencyUtils
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R as UiR

@Composable
fun FlockletCard(
    flocklet: Flocklet,
    financialSummary: FinancialSummary?,
    advice: String?,
    onEditClick: () -> Unit,
    onMoveClick: () -> Unit,
    onAddCostClick: () -> Unit,
    onAddRevenueClick: () -> Unit,
    onSellClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    showFinancials: Boolean = true
) {
    val rule = NurseryConfig.getRuleForSpecies(flocklet.species)
    val progress = (flocklet.ageInDays.toFloat() / rule.minAgeForFlock.toFloat()).coerceIn(0f, 1f)
    val isReady = flocklet.readyForFlock

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReady) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surface
        )
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
                        text = "${flocklet.chickCount} chicks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Icon - Using R from feature:flock or core:ui via dependency
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.hatchtracker.core.ui.R.drawable.hatchy_1),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).padding(end = 8.dp)
                )
                
                // Status Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isReady) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text(stringResource(UiR.string.ready_label)) },
                            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    } else if (flocklet.healthStatus != "Healthy") {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(UiR.string.health_issue_content_description),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(UiR.string.delete_action),
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
                    Text(stringResource(UiR.string.recommended_brooder_temp_label), style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = String.format("%.1f\u00B0C", flocklet.targetTemp),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Readiness Progress
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(UiR.string.time_to_graduation_label), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(UiR.string.breeding_percentage_format, (progress * 100).toInt()), style = MaterialTheme.typography.labelSmall)
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(UiR.string.financial_summary), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                        val profit = financialSummary?.profit ?: 0.0
                        Text(
                            text = stringResource(UiR.string.profit_label_format, CurrencyUtils.formatCurrency(profit)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (profit >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Row {
                        TextButton(onClick = onAddCostClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text(stringResource(UiR.string.flocklet_quick_add_cost), color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = onAddRevenueClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text(stringResource(UiR.string.flocklet_quick_add_revenue), color = Color(0xFF2E7D32))
                        }
                        TextButton(onClick = onSellClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text(stringResource(UiR.string.sell_action), color = MaterialTheme.colorScheme.primary, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                        }
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
                    Text(stringResource(UiR.string.update_status_notes_action))
                }

                if (isReady) {
                    Button(
                        onClick = onMoveClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(UiR.string.move_action))
                    }
                }
            }
        }
    }
}

