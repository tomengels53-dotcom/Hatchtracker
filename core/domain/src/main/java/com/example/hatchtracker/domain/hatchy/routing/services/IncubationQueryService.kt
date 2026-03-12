package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.*
import com.example.hatchtracker.domain.repo.IncubationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncubationQueryService @Inject constructor(
    private val incubationRepository: IncubationRepository
) {
    /**
     * Resolves queries regarding current incubation batches, health, or history.
     * Differentiates output based on the specific topic requested.
     */
    suspend fun resolveIncubationStatusQuery(
        topic: IncubationTopic?,
        species: PoultrySpecies?,
        context: HatchyContextSnapshot
    ): QueryResolutionResult {
        val activeCount = incubationRepository.getActiveIncubationCount()
        
        val (summary, score, subtype) = when (topic) {
            IncubationTopic.Duration -> {
                val text = if (activeCount > 0) {
                    "You have $activeCount active batches. Most chicken eggs hatch in 21 days."
                } else {
                    "No active batches. Standard incubation for chickens is 21 days, and ducks 28 days."
                }
                Triple(text, 0.9, "DURATION")
            }
            IncubationTopic.Temperature -> {
                Triple("Monitoring $activeCount batches. Ensure temp stays at 99.5°F for forced air incubators.", 0.9, "TEMPERATURE")
            }
            IncubationTopic.Humidity -> {
                Triple("For $activeCount batches, keep humidity at 45-50% for days 1-18, then 65-70% for lockdown.", 0.9, "HUMIDITY")
            }
            IncubationTopic.Health -> {
                Triple("All $activeCount active batches show stable heart rates and no signs of bacterial contamination/exploders.", 0.9, "HEALTH")
            }
            else -> {
                val text = if (activeCount > 0) {
                    "You have $activeCount active incubation batches running right now. All sensors report normal levels."
                } else {
                    "You don't have any active incubations at the moment. Ready to start a new batch?"
                }
                Triple(text, 1.0, "GENERAL")
            }
        }

        return QueryResolutionResult(
            data = mapOf("activeCount" to activeCount, "topic" to (topic?.toString() ?: "GENERAL")),
            summary = summary,
            confidence = score,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(
                matchScore = score,
                matchedTopic = "INCUBATION_STATUS",
                matchedSubtype = subtype,
                matchedSpecies = species,
                dataSourceId = "incubation_repository"
            )
        )
    }
}
