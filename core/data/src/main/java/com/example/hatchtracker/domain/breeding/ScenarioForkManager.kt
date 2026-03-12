package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingScenario
import com.example.hatchtracker.data.models.ScenarioStatus
import java.util.UUID

object ScenarioForkManager {

    /**
     * Creates a new scenario based on an existing one.
     * @param original The scenario to fork.
     * @param newOwnerId The UID of the user performing the fork.
     * @param forkName Optional override name for the fork.
     */
    fun forkScenario(
        original: BreedingScenario,
        newOwnerId: String,
        forkName: String? = null
    ): BreedingScenario {
        return original.copy(
            id = UUID.randomUUID().toString(),
            ownerUserId = newOwnerId,
            name = forkName ?: "Fork of ${original.name}",
            parentScenarioId = original.id,
            // If the original already had an originalAuthorAnonymized, keep it. 
            // Otherwise, credit the current owner of the original as the original author.
            originalAuthorAnonymized = original.originalAuthorAnonymized ?: "Breeder_${original.ownerUserId.takeLast(4)}",
            status = ScenarioStatus.DRAFT,
            timestamp = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis(),
            downloadCount = 0,
            isPublic = false
        )
    }

    /**
     * Helper to detect if two scenarios are related in a lineage.
     */
    fun areRelated(s1: BreedingScenario, s2: BreedingScenario): Boolean {
        return s1.id == s2.parentScenarioId || 
               s2.id == s1.parentScenarioId || 
               (s1.parentScenarioId != null && s1.parentScenarioId == s2.parentScenarioId)
    }
}
