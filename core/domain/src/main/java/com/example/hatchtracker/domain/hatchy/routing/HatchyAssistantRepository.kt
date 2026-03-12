package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*

/**
 * Interface for providing aggregate flock and incubation data to Hatchy.
 * Ensures domain logic doesn't depend on raw DAOs or specific persistence models.
 */
interface HatchyAssistantRepository {
    suspend fun getGlobalFlockSummary(): String
    suspend fun getBreedingSummary(): String
}
