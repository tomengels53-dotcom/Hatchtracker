package com.example.hatchtracker.feature.incubation

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.data.models.Incubation
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.feature.incubation.R as FeatureR


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HatchOutcomeScreen(
    incubationId: Long,
    onBackClick: () -> Unit = {},
    onHatchRecorded: () -> Unit = {},
    onAddFinancialEntry: (String, String, Boolean) -> Unit = { _, _, _ -> },
    viewModel: com.example.hatchtracker.feature.incubation.HatchOutcomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val incubation by viewModel.incubation.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()
    val error by viewModel.error.collectAsState()
    
    LaunchedEffect(incubationId) {
        viewModel.loadIncubation(incubationId)
    }
    
    LaunchedEffect(isFinished) {
        if (isFinished) onHatchRecorded()
    }

    if (incubation == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentIncubation = incubation!!
    var hatchedCount by remember { mutableFloatStateOf(0f) }
    var infertileCount by remember { mutableFloatStateOf(0f) }
    var failedCount by remember { mutableFloatStateOf(0f) }
    val caps by viewModel.capabilities.collectAsState()
    val isAdmin by UserAuthManager.isSystemAdmin.collectAsState()
    val isDeveloper by UserAuthManager.isDeveloper.collectAsState()
    val canAccessFinance = FeatureAccessPolicy
        .canAccess(FeatureKey.FINANCE, caps.tier, isAdmin || isDeveloper)
        .allowed

    var showNurseryPrompt by remember { mutableStateOf(false) }

    if (showNurseryPrompt) {
        AlertDialog(
            onDismissRequest = { showNurseryPrompt = false },
            title = { Text(stringResource(FeatureR.string.move_to_nursery_title)) },
            text = { Text(stringResource(FeatureR.string.move_to_nursery_msg_format, hatchedCount.roundToInt())) },
            confirmButton = {
                Button(onClick = {
                    showNurseryPrompt = false
                    viewModel.completeIncubation(
                        currentIncubation,
                        hatchedCount.roundToInt(),
                        infertileCount.roundToInt(),
                        failedCount.roundToInt()
                    )
                }) {
                    Text(stringResource(FeatureR.string.yes_move_to_nursery))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNurseryPrompt = false
                    viewModel.completeIncubation(
                        currentIncubation,
                        hatchedCount.roundToInt(),
                        infertileCount.roundToInt(),
                        failedCount.roundToInt()
                    )
                }) {
                    Text(stringResource(FeatureR.string.no_skip_nursery))
                }
            }
        )
    }

    val totalInput = (hatchedCount + infertileCount + failedCount).roundToInt()
    val isValidInput = totalInput <= currentIncubation.eggsCount

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.record_hatch_outcome_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.incubation_for_format, currentIncubation.species),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.total_eggs_set_format, currentIncubation.eggsCount),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Hatched Slider
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.label_hatched_format, hatchedCount.roundToInt()), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = hatchedCount,
                onValueChange = { hatchedCount = it },
                valueRange = 0f..currentIncubation.eggsCount.toFloat(),
                steps = currentIncubation.eggsCount
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Infertile Slider
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.label_infertile_format, infertileCount.roundToInt()), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = infertileCount,
                onValueChange = { infertileCount = it },
                valueRange = 0f..currentIncubation.eggsCount.toFloat(),
                steps = currentIncubation.eggsCount
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Early Death/Failed Slider
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.label_failed_format, failedCount.roundToInt()), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = failedCount,
                onValueChange = { failedCount = it },
                valueRange = 0f..currentIncubation.eggsCount.toFloat(),
                steps = currentIncubation.eggsCount
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isValidInput) {
                Text(
                    androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.error_total_exceeds_format, currentIncubation.eggsCount),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.creating_birds_format, hatchedCount.roundToInt()),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (canAccessFinance) {
                // Revenue Shortcut
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.selling_question), style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onAddFinancialEntry(currentIncubation.syncId, "incubation", true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.record_revenue_button))
                        }
                    }
                }
            }

            if (error != null) {
                Text(text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.error_generic_format, error!!), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onBackClick) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.cancel_action))
                }
                Button(
                    onClick = { 
                        if (hatchedCount > 0) {
                            showNurseryPrompt = true
                        } else {
                            viewModel.completeIncubation(
                                currentIncubation,
                                hatchedCount.roundToInt(), 
                                infertileCount.roundToInt(), 
                                failedCount.roundToInt()
                            ) 
                        }
                    },
                    enabled = isValidInput && hatchedCount >= 0
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.incubation.R.string.finalize_button))
                }
            }
        }
    }
}
