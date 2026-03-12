package com.example.hatchtracker.data.service

import com.example.hatchtracker.data.models.Incubation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compatibility shim for legacy incubation flows that still call scenario finalization.
 * Scenario finalization was moved out of this service; current behavior is a no-op.
 */
@Singleton
class BreedingScenarioService @Inject constructor() {
    suspend fun finalizeScenarioHatch(incubation: Incubation) {
        // Intentionally no-op to preserve behavior without reintroducing old architecture.
    }
}
