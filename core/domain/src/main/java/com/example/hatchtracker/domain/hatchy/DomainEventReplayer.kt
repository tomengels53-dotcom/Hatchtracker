package com.example.hatchtracker.domain.hatchy

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Replays DomainEvents to rebuild cached/derived aggregate state.
 * Part of the B2 "Rebuildable Aggregates" strategy.
 *
 * This class is intentionally dependency-light (core:domain has no reference to core:data).
 * Callers in core:data inject and call EggProductionRepository directly.
 * This class only provides the rebuild order contract and routing logic via the RebuildDelegate.
 *
 * Rebuild order (must be preserved per flock):
 * 1. rebuildSetForIncubation — restores egg reservation counts
 * 2. rebuildSoldEggs        — restores sale allocation counts (must come after reservations)
 */
@Singleton
class DomainEventReplayer @Inject constructor(
    private val delegate: RebuildDelegate
) {
    /**
     * Domain-agnostic delegate. Implemented in core:data by a Hilt-injected concrete type.
     */
    interface RebuildDelegate {
        suspend fun rebuildSetForIncubation(flockId: String)
        suspend fun rebuildSoldEggs(flockId: String)
        suspend fun getAllFlockIds(): List<String>
    }

    /**
     * Rebuilds cached state for a specific flock aggregate.
     * aggregateId must be the flockId as a String UUID.
     */
    suspend fun rebuildAggregate(aggregateType: String, aggregateId: String) {
        when (aggregateType) {
            "FLOCK" -> {
                delegate.rebuildSetForIncubation(aggregateId)
                delegate.rebuildSoldEggs(aggregateId)
            }
            // Other aggregate types: add cases here as needed
        }
    }

    /**
     * Full system rebuild: all flocks, correct order.
     */
    suspend fun rebuildAll() {
        val flockIds = delegate.getAllFlockIds()
        flockIds.forEach { flockId ->
            delegate.rebuildSetForIncubation(flockId)
            delegate.rebuildSoldEggs(flockId)
        }
    }
}
