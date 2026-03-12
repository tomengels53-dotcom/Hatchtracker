package com.example.hatchtracker.data.util

import com.example.hatchtracker.data.models.FinancialEntry
import java.util.*
import java.util.Calendar

object FinancialChartUtil {

    enum class TimeBucket { DAY, WEEK, MONTH }

    data class ChartPoint(
        val label: String,
        val timestamp: Long,
        val totalCost: Double,
        val feedCost: Double,
        val revenue: Double
    )

    fun aggregate(
        entries: List<FinancialEntry>,
        bucketType: TimeBucket,
        startDate: Long,
        endDate: Long
    ): List<ChartPoint> {
        val calendar = Calendar.getInstance()
        val groupedEntries = entries.groupBy { entry ->
            calendar.timeInMillis = entry.date
            truncateToBucket(calendar, bucketType)
        }

        val result = mutableListOf<ChartPoint>()
        var current = Calendar.getInstance().apply {
            timeInMillis = startDate
            truncateToBucket(this, bucketType)
        }
        
        val end = Calendar.getInstance().apply {
            timeInMillis = endDate
            truncateToBucket(this, bucketType)
        }

        while (current.timeInMillis <= end.timeInMillis) {
            val key = current.timeInMillis
            val bucketEntries = groupedEntries[key] ?: emptyList()
            
            val totalCost = bucketEntries.filter { it.type == "cost" }.sumOf { it.amount }
            val feedCost = bucketEntries.filter { it.category == "feed" }.sumOf { it.amount }
            val revenue = bucketEntries.filter { it.type == "revenue" }.sumOf { it.amount }

            result.add(
                ChartPoint(
                    label = formatLabel(current, bucketType),
                    timestamp = key,
                    totalCost = totalCost,
                    feedCost = feedCost,
                    revenue = revenue
                )
            )

            // Advance to next bucket
            when (bucketType) {
                TimeBucket.DAY -> current.add(Calendar.DAY_OF_YEAR, 1)
                TimeBucket.WEEK -> current.add(Calendar.WEEK_OF_YEAR, 1)
                TimeBucket.MONTH -> current.add(Calendar.MONTH, 1)
            }
        }

        return result
    }

    private fun truncateToBucket(calendar: Calendar, bucketType: TimeBucket): Long {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        if (bucketType == TimeBucket.WEEK) {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        } else if (bucketType == TimeBucket.MONTH) {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }
        
        return calendar.timeInMillis
    }

    private fun formatLabel(calendar: Calendar, bucketType: TimeBucket): String {
        return when (bucketType) {
            TimeBucket.DAY -> "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}"
            TimeBucket.WEEK -> "W${calendar.get(Calendar.WEEK_OF_YEAR)}"
            TimeBucket.MONTH -> "${calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())}"
        }
    }
}

