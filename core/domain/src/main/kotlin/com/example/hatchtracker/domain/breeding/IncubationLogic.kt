@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.core.common.IncubationRegistry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object IncubationLogic {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Calculates the expected hatch date based on species-specific duration.
     */
    fun calculateExpectedHatchDate(startDateStr: String, species: String): String {
        return try {
            val startDate = LocalDate.parse(startDateStr, dateFormatter)
            val profile = IncubationRegistry.getProfileForSpecies(species)
            val hatchDate = startDate.plusDays(profile.incubationDurationDays.toLong())
            hatchDate.format(dateFormatter)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Checks if a given day is the start of lockdown.
     */
    fun isLockdownDay(currentDayOfIncubation: Int, species: String): Boolean {
        val profile = IncubationRegistry.getProfileForSpecies(species)
        return currentDayOfIncubation >= profile.lockdownStartDay
    }
}
