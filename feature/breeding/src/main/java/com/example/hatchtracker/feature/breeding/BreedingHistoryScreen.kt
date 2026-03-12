package com.example.hatchtracker.feature.breeding

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.BreedingRecord
import com.example.hatchtracker.domain.breeding.TraitProgressionPoint
import com.example.hatchtracker.domain.breeding.ConfidencePoint
import com.example.hatchtracker.domain.breeding.BreedingAnalyticsManager
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingHistoryScreen(
    onBack: () -> Unit,
    canAccessBreeding: Boolean,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: com.example.hatchtracker.feature.breeding.BreedingHistoryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    // Check PRO access
    if (!canAccessBreeding) {
        BreedingLockedScreen(
            onBack = onBack,
            onViewPlans = onNavigateToPaywall
        )
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<BreedingRecord?>(null) }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(UiR.string.delete_breeding_record_confirm_title)) },
            text = { Text(stringResource(UiR.string.delete_breeding_record_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = showDeleteDialog
                        if (toDelete != null) {
                            viewModel.deleteRecord(toDelete)
                        }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(UiR.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(UiR.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.breeding_analytics_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Trait Progression Section
                item {
                    AnalyticsCard(title = stringResource(UiR.string.trait_progression_generation_title)) {
                        uiState.traitProgression.forEach { (trait, points) ->
                            TraitProgressionChart(trait, points)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // 2. Confidence Growth Section
                item {
                    AnalyticsCard(title = stringResource(UiR.string.genetic_confidence_growth_title)) {
                        ConfidenceGrowthChart(uiState.confidenceTrend)
                    }
                }

                // 3. Past Breeding Pairs
                item {
                    Text(
                        stringResource(UiR.string.mating_history_title),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                }

                items(uiState.records) { record ->
                    HistoricalRecordCard(
                        record = record, 
                        birds = uiState.birds, 
                        incubations = uiState.incubations,
                        onDeleteClick = { showDeleteDialog = record }
                    )
                }
                
                if (uiState.records.isEmpty()) {
                    item {
                        Text(
                            stringResource(UiR.string.no_past_breeding_records),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun TraitProgressionChart(trait: String, points: List<TraitProgressionPoint>) {
    Column {
        Text(trait, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            points.forEach { point ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(point.prevalence.coerceIn(0.01f, 1f))
                                .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(UiR.string.breeding_generation_label, point.generation), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(UiR.string.breeding_percentage_format, (point.prevalence * 100).toInt()), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
            }
        }
    }
}

@Composable
fun ConfidenceGrowthChart(points: List<ConfidencePoint>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 8.dp)) {
        if (points.size < 2) return@Canvas
        
        val width = size.width
        val height = size.height
        val maxPoints = points.size
        
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = (index.toFloat() / (maxPoints - 1)) * width
            val y = height - (point.averageConfidence * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Draw dots
        points.forEachIndexed { index, point ->
            val x = (index.toFloat() / (maxPoints - 1)) * width
            val y = height - (point.averageConfidence * height)
            drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun HistoricalRecordCard(
    record: BreedingRecord,
    birds: List<Bird>,
    incubations: List<Incubation>,
    onDeleteClick: () -> Unit = {}
) {
    val sire = birds.find { it.id == record.sireId }
    val damsCount = record.damIds.size
    val stats = BreedingAnalyticsManager.calculatePairStats(
        record.sireId,
        record.damIds,
        incubations
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Column {
                        Text(stringResource(UiR.string.breeding_sire_label, sire?.breed ?: stringResource(UiR.string.unknown_label)), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                        Text(stringResource(UiR.string.dams_count_species_format, damsCount, record.species), style = MaterialTheme.typography.bodySmall)
                    }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                     Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (stats.hatchRate > 0.7f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            stringResource(UiR.string.success_rate_format, (stats.hatchRate * 100).toInt()),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                        )
                    }
                    
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(UiR.string.delete_record_content_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
            }
            
            if (record.goals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(UiR.string.goals_label_format, record.goals.joinToString(", ")), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}








