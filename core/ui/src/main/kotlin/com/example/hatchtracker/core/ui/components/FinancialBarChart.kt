package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.util.FinancialChartUtil
import com.example.hatchtracker.core.ui.theme.profit
import com.example.hatchtracker.core.ui.theme.loss

@Composable
fun FinancialBarChart(
    data: List<FinancialChartUtil.ChartPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxVal = (data.maxByOrNull { maxOf(it.revenue, it.totalCost) }?.let { maxOf(it.revenue, it.totalCost) } ?: 1.0).coerceAtLeast(1.0)

    val profitColor = MaterialTheme.colorScheme.profit
    val lossColor = MaterialTheme.colorScheme.loss

    Column(modifier = modifier) {
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / (data.size * 3)
            val spacing = barWidth

            data.forEachIndexed { index, point ->
                val xBase = (index * 3 * barWidth) + spacing
                
                // Cost Bar
                val costHeight = (point.totalCost / maxVal).toFloat() * canvasHeight
                drawRect(
                    color = lossColor, // Light Red
                    topLeft = Offset(xBase, canvasHeight - costHeight),
                    size = Size(barWidth, costHeight)
                )

                // Revenue Bar
                val revHeight = (point.revenue / maxVal).toFloat() * canvasHeight
                drawRect(
                    color = profitColor, // Light Green
                    topLeft = Offset(xBase + barWidth, canvasHeight - revHeight),
                    size = Size(barWidth, revHeight)
                )
            }

            // Zero Line
            drawLine(
                color = Color.Gray,
                start = Offset(0f, canvasHeight),
                end = Offset(canvasWidth, canvasHeight),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Labels (Simple)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.takeLast(5).forEach { point ->
                Text(text = point.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}




