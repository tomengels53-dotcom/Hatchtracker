package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.ConfidenceEvent
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConfidenceTrendGraph(
    events: List<ConfidenceEvent>,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(stringResource(com.example.hatchtracker.core.ui.R.string.no_history_available), style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    // Sort by timestamp just in case
    val sortedEvents = events.sortedBy { it.timestamp }
    
    // Simulate initial score of 0 if first event is a delta
    // For visualization, we'll just plot the cumulative score
    var cumulative = 0.0
    val points = sortedEvents.map { 
        cumulative = (cumulative + it.delta).coerceIn(0.0, 100.0)
        Pair(it.timestamp, cumulative)
    }

    Column(modifier = modifier) {
        Text(stringResource(com.example.hatchtracker.core.ui.R.string.confidence_trend_title), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        val primaryColor = MaterialTheme.colorScheme.primary
        
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(start = 24.dp, bottom = 24.dp) // Space for axes
        ) {
            val width = size.width
            val height = size.height
            
            // Draw Axis
            drawLine(
                color = Color.Gray,
                start = Offset(0f, 0f),
                end = Offset(0f, height),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Gray,
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 2f
            )
            
            if (points.size < 2) return@Canvas

            val minTime = points.first().first
            val maxTime = points.last().first
            val timeRange = (maxTime - minTime).coerceAtLeast(1) // Avoid div/0
            
            val path = Path()
            
            points.forEachIndexed { index, (time, score) ->
                val x = ((time - minTime).toFloat() / timeRange) * width
                val y = height - ((score.toFloat() / 100f) * height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                
                // Draw dots
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx())
            )
            
            // Y-Axis Labels (0, 50, 100)
            // Note: Canvas doesn't support drawing text easily in raw Compose Canvas without NativeCanvas access 
            // commonly done via drawIntoCanvas. Providing simplified grid lines instead.
            
            val y50 = height - (0.5f * height)
            drawLine(
                color = Color.LightGray,
                start = Offset(0f, y50),
                end = Offset(width, y50),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

        }
        
        // Simple X-Axis Label support
        if (points.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDate(points.first().first), style = MaterialTheme.typography.labelSmall)
                Text(formatDate(points.last().first), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
