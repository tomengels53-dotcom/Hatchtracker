package com.example.hatchtracker.feature.breeding.actionplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant

import com.example.hatchtracker.data.models.BreedingProgram
import com.example.hatchtracker.core.ui.R as UiR
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingProgramHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: BreedingProgramHubViewModel = hiltViewModel()
) {
    val plans by viewModel.plans.collectAsState()

    Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(UiR.string.breeding_action_plans_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.back_action))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (plans.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(UiR.string.breeding_no_active_plans), style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(plans) { plan ->
                            BreedingProgramItem(
                                plan = plan,
                                onOpen = { onNavigateToDetail(plan.id) },
                                onArchive = { viewModel.archivePlan(plan.id) },
                                onDelete = { viewModel.deletePlan(plan.id) }
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun BreedingProgramItem(
    plan: BreedingProgram,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(variant = AppCardVariant.STANDARD) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(plan.updatedAt)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (plan.scenarioId.isNotBlank()) {
                Text(
                    stringResource(UiR.string.breeding_from_scenario, plan.scenarioId),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onArchive) {
                    Icon(Icons.Default.Archive, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(UiR.string.breeding_archive_action))
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(UiR.string.delete_action))
                }
                Button(onClick = onOpen) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(UiR.string.breeding_open_action))
                }
            }
        }
    }
}
