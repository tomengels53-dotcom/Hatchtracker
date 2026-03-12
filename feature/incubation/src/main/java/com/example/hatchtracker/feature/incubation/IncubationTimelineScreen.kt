package com.example.hatchtracker.feature.incubation

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.core.common.DayStatus
import com.example.hatchtracker.core.common.IncubationManager
import com.example.hatchtracker.core.common.IncubationPhase
import com.example.hatchtracker.core.common.IncubationTask
import com.example.hatchtracker.core.common.TimelineDayState
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.feature.incubation.R as FeatureR
import com.example.hatchtracker.core.ui.R as UiR

import com.example.hatchtracker.common.format.LocaleFormatService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncubationTimelineScreen(
    incubation: Incubation,
    dateFormat: String,
    localeFormatService: LocaleFormatService,
    onBackClick: () -> Unit
) {
    val timeline = remember(incubation) { IncubationManager.generateTimeline(incubation) }
    val todayIndex = timeline.indexOfFirst { it.status == DayStatus.TODAY }.takeIf { it != -1 } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = todayIndex.coerceAtLeast(0))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(FeatureR.string.timeline_title_format, incubation.species)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(UiR.string.back_action)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(timeline) { dayState ->
                TimelineDayItem(dayState, dateFormat, localeFormatService)
            }
        }
    }
}

@Composable
fun TimelineDayItem(dayState: TimelineDayState, dateFormat: String, localeFormatService: LocaleFormatService) {
    val isToday = dayState.status == DayStatus.TODAY
    val isPast = dayState.status == DayStatus.PAST
    
    val primaryColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isPast -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    }

    Row(
        modifier = Modifier.height(IntrinsicSize.Min)
    ) {
        // Left Connector Logic
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            // Top Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .weight(1f)
                    .background(if (dayState.dayNumber == 1) Color.Transparent else MaterialTheme.colorScheme.outlineVariant)
            )
            
            // Node
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isToday) 32.dp else 24.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            ) {
                Text(
                    text = "${dayState.dayNumber}",
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                )
            }
            
            // Bottom Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        
        // Right Content
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .padding(bottom = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 0.dp),
            border = if (!isToday) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = localeFormatService.formatDate(dayState.date.toString(), dateFormat),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (dayState.phase == IncubationPhase.LOCKDOWN) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text(stringResource(FeatureR.string.lockdown_label), color = Color.White)
                        }
                    } else if (dayState.phase == IncubationPhase.HATCH_WINDOW) {
                        Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                            Text(stringResource(FeatureR.string.hatching_label), color = Color.White)
                        }
                    }
                }
                
                // Tasks
                if (dayState.tasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    dayState.tasks.forEach { task ->
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                "\u2022",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getLocalizedTaskDescription(task),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (task.isCritical) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getLocalizedTaskDescription(task: IncubationTask): String {
    val key = task.descriptionKey ?: return task.description
    val resId = when (key) {
        "task_turn_eggs" -> FeatureR.string.task_turn_eggs
        "task_stop_turning" -> FeatureR.string.task_stop_turning
        "task_cool_eggs_format" -> FeatureR.string.task_cool_eggs_format
        "task_mist_eggs" -> FeatureR.string.task_mist_eggs
        "task_candle_eggs_format" -> FeatureR.string.task_candle_eggs_format
        "task_lockdown_mode" -> FeatureR.string.task_lockdown_mode
        "task_humidity_increase_format" -> FeatureR.string.task_humidity_increase_format
        "task_ventilation_increase" -> FeatureR.string.task_ventilation_increase
        "task_check_params" -> FeatureR.string.task_check_params
        "task_turn_eggs_simple" -> FeatureR.string.task_turn_eggs_simple
        "task_final_turn" -> FeatureR.string.task_final_turn
        "task_enter_lockdown" -> FeatureR.string.task_enter_lockdown
        else -> null
    }
    return if (resId != null) {
        stringResource(resId, *task.descriptionArgs.toTypedArray())
    } else {
        task.description
    }
}
