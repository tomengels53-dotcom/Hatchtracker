package com.example.hatchtracker.data.service

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.repository.EggProductionRepository
import com.example.hatchtracker.domain.hatchy.DomainEventReplayer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of DomainEventReplayer.RebuildDelegate.
 * Lives in core:data — has access to both AppDatabase and EggProductionRepository.
 * Injected into DomainEventReplayer via Hilt.
 */
@Singleton
class EggProductionRebuildDelegate @Inject constructor(
    private val db: AppDatabase,
    private val eggProductionRepository: EggProductionRepository
) : DomainEventReplayer.RebuildDelegate {

    override suspend fun rebuildSetForIncubation(flockId: String) {
        eggProductionRepository.rebuildSetForIncubationForFlock(flockId)
    }

    override suspend fun rebuildSoldEggs(flockId: String) {
        eggProductionRepository.rebuildSoldEggsForFlock(flockId)
    }

    override suspend fun getAllFlockIds(): List<String> {
        return db.flockDao().getAllFlockEntitysSync().map { it.cloudId }
    }
}
