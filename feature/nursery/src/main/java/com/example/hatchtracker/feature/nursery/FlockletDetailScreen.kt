package com.example.hatchtracker.feature.nursery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant
import com.example.hatchtracker.core.ui.components.FinancialSummaryCard

import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlockletDetailScreen(
    flockletId: Long,
    onBackClick: () -> Unit,
    onNavigateToFinance: (String, String) -> Unit,
    onAddFinancialEntry: (String, String, Boolean) -> Unit,
    onRecordSale: (String, String) -> Unit,
    onGraduateToFlock: (String, List<String>, Long) -> Unit,
    viewModel: FlockletDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(flockletId) {
        viewModel.setFlockletId(flockletId)
    }

    val flocklet by viewModel.flocklet.collectAsState()
    val financialSummary by viewModel.financialSummary.collectAsState()
    val caps by viewModel.capabilities.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isDeveloper by viewModel.isDeveloper.collectAsState()

    val canAccessFinance = FeatureAccessPolicy
        .canAccess(FeatureKey.FINANCE, caps.tier, isAdmin || isDeveloper)
        .allowed

    Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(flocklet?.let { "${it.species} Batch" } ?: "Flocklet Details") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                        }
                    }
                )
            }
        ) { paddingValues ->
            flocklet?.let { f ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = AppCardVariant.STANDARD
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Batch of ${f.chickCount} chicks",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Age: ${f.ageInDays} days • Breeds: ${f.breeds.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (f.readyForFlock) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { onGraduateToFlock(f.species, f.breeds, f.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Graduate to Adult Flock")
                                    }
                                }
                            }
                        }
                    }

                    if (canAccessFinance) {
                        item {
                            FinancialSummaryCard(
                                summary = financialSummary,
                                modifier = Modifier.fillMaxWidth(),
                                currencyCode = currencyCode,
                                onAddCostClick = { onAddFinancialEntry(f.syncId, "flocklet", false) },
                                onAddRevenueClick = { onAddFinancialEntry(f.syncId, "flocklet", true) },
                                onDeepdiveClick = { onNavigateToFinance(f.syncId, "flocklet") },
                                tier = caps.tier,
                                isAdmin = isAdmin,
                                isDeveloper = isDeveloper
                            )
                        }
                    }

                    f.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        item {
                            AppCard(
                                modifier = Modifier.fillMaxWidth(),
                                variant = AppCardVariant.SUBTLE
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Notes", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(notes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    
                    item {
                        OutlinedButton(
                            onClick = { onRecordSale(f.syncId, "flocklet") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Record Sale")
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
    }
}
}
