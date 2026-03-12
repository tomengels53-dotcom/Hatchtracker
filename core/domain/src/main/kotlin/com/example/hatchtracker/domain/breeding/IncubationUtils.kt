@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.domain.breeding

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Utility functions for incubation calculations.
 */
object IncubationUtils {
    
    /**
     * Calculates the number of days until the expected hatch date.
     * Returns negative if past the expected date.
     */
    fun calculateDaysUntilHatch(expectedHatch: String): Int {
        return try {
            val expected = LocalDate.parse(expectedHatch)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, expected).toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Calculates the incubation progress as a percentage (0.0 to 1.0).
     */
    fun calculateIncubationProgress(startDate: String, expectedHatch: String): Float {
        return try {
            val start = LocalDate.parse(startDate)
            val expected = LocalDate.parse(expectedHatch)
            val today = LocalDate.now()
            
            val totalDays = ChronoUnit.DAYS.between(start, expected).toFloat()
            if (totalDays <= 0) return 1f
            
            val elapsedDays = ChronoUnit.DAYS.between(start, today).toFloat()
            (elapsedDays / totalDays).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Calculates the fertility rate (ratio of fertile eggs to total eggs).
     * Returns 0f if no eggs were set.
     */
    fun calculateFertilityRate(totalSet: Int, infertile: Int): Float {
        if (totalSet <= 0) return 0f
        return (totalSet - infertile).coerceAtLeast(0).toFloat() / totalSet.toFloat()
    }

    /**
     * Calculates the hatchability (ratio of hatched eggs to fertile eggs).
     * Returns 0f if no fertile eggs were found.
     */
    fun calculateHatchability(hatched: Int, totalSet: Int, infertile: Int): Float {
        val fertile = totalSet - infertile
        if (fertile <= 0) return 0f
        return hatched.coerceAtLeast(0).toFloat() / fertile.toFloat()
    }

    /**
     * Formats a rate (0.0 to 1.0) as a user-friendly percentage string (e.g., "85%").
     */
    fun formatPercentage(rate: Float): String {
        return "${(rate * 100).toInt().coerceIn(0, 100)}%"
    }

    /**
     * Formats an ISO-8601 date string (YYYY-MM-DD) to the user's local date format.
     */
    fun formatDateForDisplay(isoDateString: String): String {
        return try {
            val date = LocalDate.parse(isoDateString)
            val formatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
            date.format(formatter)
        } catch (e: Exception) {
            isoDateString // Fallback to raw string if parse fails
        }
    }
}
