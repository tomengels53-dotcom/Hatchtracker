@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.feature.breeding

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.TraitPromotion
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraitPromotionScreen(
    onBack: () -> Unit,
    canAccessBreeding: Boolean,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: TraitPromotionViewModel = hiltViewModel()
) {
    if (!canAccessBreeding) {
        BreedingLockedScreen(
            onBack = onBack,
            onViewPlans = onNavigateToPaywall
        )
        return
    }

    val birdsWithInferredTraits by viewModel.birdsWithInferredTraits.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.admin_genetic_tools_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(stringResource(UiR.string.promotions_title), modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(stringResource(UiR.string.audit_trail_title), modifier = Modifier.padding(16.dp))
                }
            }

            if (selectedTab == 0) {
                PromotionList(
                    birds = birdsWithInferredTraits,
                    onPromote = { bird, trait ->
                        viewModel.promoteTrait(bird, trait, "SYSTEM_ADMIN", "Community validation")
                    }
                )
            } else {
                AuditTrailList(logs = auditLogs)
            }
        }
    }
}

@Composable
fun PromotionList(
    birds: List<Bird>,
    onPromote: (Bird, String) -> Unit
) {
    if (birds.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(UiR.string.no_inferred_traits_pending))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(birds) { bird ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${bird.breed} (${bird.id})", style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(UiR.string.current_inferred_traits), style = MaterialTheme.typography.bodySmall)
                        
                        bird.geneticProfile.inferredTraits.forEach { trait ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(trait, style = MaterialTheme.typography.bodyMedium)
                                ElevatedButton(
                                    onClick = { onPromote(bird, trait) }
                                ) {
                                    Text(stringResource(UiR.string.promote_to_fixed_action), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditTrailList(logs: List<TraitPromotion>) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(UiR.string.no_activity_recorded_yet))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                "Promoted '${log.traitName}' (Bird ${log.birdId})",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                            )
                            Text(
                                "By: ${log.promotedBy} at ${java.time.Instant.ofEpochMilli(log.timestamp)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (log.reason != null) {
                                Text(stringResource(UiR.string.reason_prefix, log.reason.orEmpty()), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}





