package com.example.hatchtracker.feature.breeding.actionplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant

import com.example.hatchtracker.data.models.AssetType
import com.example.hatchtracker.data.models.LinkRole
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingProgramLinkAssetsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BreedingProgramDetailViewModel = hiltViewModel()
) {
    val plan by viewModel.plan.collectAsState()
    
    val mockFlocks = listOf("F1: Master Breeding Pen", "F2: Araucana Group", "F3: Black Cooper Group")
    val mockFlocklets = listOf("FL1: 2026 Batch A", "FL2: 2026 Batch B")

    Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(UiR.string.breeding_link_assets)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.back_action))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text(stringResource(UiR.string.available_flocks_title), style = MaterialTheme.typography.titleMedium) }
                items(mockFlocks) { flock ->
                    AppCard(variant = AppCardVariant.STANDARD) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(flock.substringAfter(": "))
                            TextButton(onClick = { /* link logic */ }) {
                                Text(stringResource(UiR.string.link_as_sire_action))
                            }
                        }
                    }
                }
                
                item { Text(stringResource(UiR.string.available_flocklets_title), style = MaterialTheme.typography.titleMedium) }
                items(mockFlocklets) { flocklet ->
                    AppCard(variant = AppCardVariant.STANDARD) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(flocklet.substringAfter(": "))
                            TextButton(onClick = { /* link logic */ }) {
                                Text(stringResource(UiR.string.link_as_dam_action))
                            }
                        }
                    }
                }
            }
        }
    }
