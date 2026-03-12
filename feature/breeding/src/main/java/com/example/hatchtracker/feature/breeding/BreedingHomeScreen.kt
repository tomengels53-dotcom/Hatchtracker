// LAYOUT COMPOSITION CONTRACT
// This screen is restored to baseline commit 217a6a11.
// Do NOT alter:
// - Section ordering
// - Hero prominence
// - Grid structure
// - Spacing rhythm (24dp vertical / 16dp edges)
// - Card variant types
// without explicit design approval.
// LAYOUT COMPOSITION CONTRACT
// This screen conforms strictly to the pre-polish layout baseline.
// Do NOT alter container structure or component layout without explicit design approval.
package com.example.hatchtracker.feature.breeding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant
import com.example.hatchtracker.core.ui.composeutil.premiumClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingHomeScreen(
    onNavigateToInsights: () -> Unit,
    onNavigateToPrograms: () -> Unit,
    onNavigateToWizard: () -> Unit,
    onNavigateToHatchy: () -> Unit,
    viewModel: BreedingHomeViewModel = hiltViewModel()
) {
    val planCount by viewModel.planCount.collectAsState()

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Breeding Management",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Breeding Programs (Hero)
                AppCard(
                    variant = AppCardVariant.HERO,
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumClickable(onClick = onNavigateToPrograms)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Badge { Text("$planCount") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.breeding_hub_programs_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.breeding_hub_programs_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Breeding Wizard (Standard)
                AppCard(
                    variant = AppCardVariant.STANDARD,
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumClickable(onClick = onNavigateToWizard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.breeding_hub_wizard_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.breeding_hub_wizard_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Breed Library (Standard)
                AppCard(
                    variant = AppCardVariant.STANDARD,
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumClickable(onClick = onNavigateToInsights)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.breeding_hub_library_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(R.string.breeding_hub_library_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Hatchy Chat Bubble
            com.example.hatchtracker.core.ui.components.HatchyAssistButton(
                onClick = onNavigateToHatchy,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 24.dp)
            )
        }
    }
}
