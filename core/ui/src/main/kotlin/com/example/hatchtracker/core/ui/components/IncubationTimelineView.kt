@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.core.common.*
import java.time.LocalDate

@Composable
fun IncubationTimelineView(
    incubation: Incubation,
    deviceFeatures: com.example.hatchtracker.data.models.DeviceFeatures? = null,
    modifier: Modifier = Modifier
) {
    val timeline = remember(incubation, deviceFeatures) {
        IncubationTimelineEngine.generateTimeline(incubation, deviceFeatures)
    }
    val today = remember { LocalDate.now() }
    val todayDay = timeline.find { it.date == today }
    val listState = rememberLazyListState()

    // Scroll to today on first load
    LaunchedEffect(timeline) {
        val todayIndex = timeline.indexOfFirst { it.date == today }
        if (todayIndex != -1) {
            listState.scrollToItem(todayIndex)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(com.example.hatchtracker.core.ui.R.string.hatch_timeline_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(com.example.hatchtracker.core.ui.R.string.incubation_started_with_species_format, incubation.species, incubation.startDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Horizonatal Day Selector
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timeline) { day ->
                    DayMarker(
                        day = day,
                        isToday = day.date == today,
                        isPast = day.date.isBefore(today)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Detailed view for Today or selected day (defaults to today)
            if (todayDay != null) {
                TodayDetails(day = todayDay)
            } else {
                // If hatch is finished or not started
                val status = IncubationManager.getStatus(incubation)
                Text(
                    text = stringResource(com.example.hatchtracker.core.ui.R.string.project_status_format, status.phase.name.replace("_", " ")),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DayMarker(
    day: TimelineDay,
    isToday: Boolean,
    isPast: Boolean
) {
    val color = when (day.phase) {
        IncubationPhase.EARLY_INCUBATION -> MaterialTheme.colorScheme.primary
        IncubationPhase.MID_INCUBATION -> MaterialTheme.colorScheme.secondary
        IncubationPhase.LOCKDOWN -> MaterialTheme.colorScheme.error
        IncubationPhase.HATCH_WINDOW -> Color(0xFF4CAF50) // Green
        IncubationPhase.OVERDUE -> MaterialTheme.colorScheme.error
        IncubationPhase.COMPLETE -> MaterialTheme.colorScheme.tertiary
    }

    val alpha = if (isPast && !isToday) 0.4f else 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(50.dp)
    ) {
        Text(
            text = "D${day.dayNumber}",
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isToday) color else color.copy(alpha = 0.2f * alpha))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPast && !isToday) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            } else if (day.actions.any { it.isCritical }) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isToday) Color.White else color,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) Color.White else color
                )
            }
        }
        
        if (isToday) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun TodayDetails(day: TimelineDay) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(com.example.hatchtracker.core.ui.R.string.phase_format, day.phase.name.replace("_", " ")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (day.actions.isEmpty()) {
                Text(stringResource(com.example.hatchtracker.core.ui.R.string.standard_incubation_no_special_actions), style = MaterialTheme.typography.bodySmall)
            } else {
                day.actions.forEach { action ->
                    ActionItem(action)
                }
            }
        }
    }
}

@Composable
fun ActionItem(action: TimelineAction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        val color = if (action.isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}




