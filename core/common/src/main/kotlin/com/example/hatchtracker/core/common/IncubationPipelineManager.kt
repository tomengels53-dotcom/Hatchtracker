@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.core.common

import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.BreedingScenario
import com.example.hatchtracker.data.models.ScenarioStatus
import com.example.hatchtracker.model.UiText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class IncubationCheckpoint(
    val day: Int,
    val title: String,
    val description: String,
    val hatchyNote: UiText
)

object IncubationPipelineManager {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun createIncubationFromScenario(
        scenario: BreedingScenario,
        numEggs: Int,
        startDate: LocalDate = LocalDate.now()
    ): Incubation {
        val speciesDuration = getDurationForSpecies(scenario.species)
        val expectedHatch = startDate.plusDays(speciesDuration.toLong())

        return Incubation(
            flockId = null, // Can be linked later by user
            species = scenario.species,
            breeds = listOf(scenario.name),
            startDate = startDate.format(dateFormatter),
            expectedHatch = expectedHatch.format(dateFormatter),
            eggsCount = numEggs,
            sourceScenarioId = scenario.id,
            incubationProfileId = "standard_${scenario.species.lowercase()}",
            hatchNotes = "Generated from breeding scenario: ${scenario.name}"
        )
    }

    fun generateTimeline(species: String, startDate: String): List<IncubationCheckpoint> {
        val start = LocalDate.parse(startDate, dateFormatter)
        
        return when (species.lowercase()) {
            "chicken" -> listOf(
                IncubationCheckpoint(
                    day = 7,
                    title = "First Candling",
                    description = "Check for development and remove clears.",
                    hatchyNote = HatchyGuidanceEngine.getIncubationTip(7)
                ),
                IncubationCheckpoint(
                    day = 14,
                    title = "Final Candling",
                    description = "Verify air cell growth and movement.",
                    hatchyNote = HatchyGuidanceEngine.getIncubationTip(14)
                ),
                IncubationCheckpoint(
                    day = 18,
                    title = "Lockdown",
                    description = "Stop turning. Increase humidity. Do not open the incubator.",
                    hatchyNote = HatchyGuidanceEngine.getIncubationTip(18)
                ),
                IncubationCheckpoint(
                    day = 21,
                    title = "Estimated Hatch",
                    description = "Chicks should start pipping.",
                    hatchyNote = UiText.DynamicString("Trust the birds. They know what to do.")
                )
            )
            else -> listOf(
                IncubationCheckpoint(
                    day = 1,
                    title = "Incubation Start",
                    description = "Maintain steady temp and humidity.",
                    hatchyNote = UiText.DynamicString("Every journey starts with a single egg.")
                )
            )
        }
    }

    private fun getDurationForSpecies(species: String): Int {
        return when (species.lowercase()) {
            "chicken" -> 21
            "duck" -> 28
            "quail" -> 17
            "turkey" -> 28
            "goose" -> 30
            else -> 21
        }
    }
}


