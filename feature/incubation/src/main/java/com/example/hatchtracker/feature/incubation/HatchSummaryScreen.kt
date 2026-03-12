package com.example.hatchtracker.feature.incubation

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.Sex
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.feature.incubation.R as FeatureR
import com.example.hatchtracker.core.ui.R as UiR

@Composable
fun HatchSummaryScreen(
    incubation: Incubation,
    draftBirds: List<Bird>,
    stats: HatchStats,
    onBirdChange: (Bird) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        bottomBar = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Text(stringResource(FeatureR.string.confirm_add_to_flock_action))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(FeatureR.string.hatch_summary_title), style = MaterialTheme.typography.headlineMedium)
            }

            // Stats Card
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(FeatureR.string.hatch_rate_label), style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${stats.hatchRate}%", 
                                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(FeatureR.string.summary_label), style = MaterialTheme.typography.labelMedium)
                            Text("${stringResource(FeatureR.string.total_eggs_set_format, stats.totalSet)}", style = MaterialTheme.typography.bodyMedium)
                            Text("${stringResource(FeatureR.string.label_hatched_format, stats.hatched)}", style = MaterialTheme.typography.bodyMedium)
                            Text("${stringResource(FeatureR.string.label_failed_format, stats.failed)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(FeatureR.string.review_new_members_format, draftBirds.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(draftBirds) { bird ->
                DraftBirdCard(bird = bird, onEdit = { onBirdChange(it) })
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DraftBirdCard(bird: Bird, onEdit: (Bird) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(bird.notes ?: "") } // Using notes as temp name holder

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(FeatureR.string.name_id_label)) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name.ifBlank { stringResource(FeatureR.string.unnamed_chick) }, style = MaterialTheme.typography.titleMedium)
                        Text("${bird.species} \u2022 ${stringResource(FeatureR.string.unsexed_label)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                IconButton(onClick = { 
                    if (isEditing) {
                        onEdit(bird.copy(notes = name))
                        isEditing = false
                    } else {
                        isEditing = true
                    }
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = stringResource(UiR.string.edit_action)
                    )
                }
            }
        }
    }
}

data class HatchStats(
    val totalSet: Int,
    val hatched: Int,
    val infertile: Int,
    val failed: Int
) {
    val hatchRate: Int
        get() = if (totalSet > 0) ((hatched.toFloat() / totalSet) * 100).toInt() else 0
}
