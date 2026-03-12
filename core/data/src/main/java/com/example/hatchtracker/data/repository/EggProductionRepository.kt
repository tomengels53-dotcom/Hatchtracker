@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.EggProductionDao
import com.example.hatchtracker.data.models.EggProductionEntity
import com.example.hatchtracker.model.SyncState
import com.google.firebase.auth.FirebaseAuth
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EggProductionRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: EggProductionDao,
    private val syncCoordinator: com.example.hatchtracker.data.sync.CoreDataSyncCoordinator,
    private val unitCostProvider: com.example.hatchtracker.domain.pricing.UnitCostProvider,
    private val domainEventLogger: com.example.hatchtracker.data.DomainEventLogger,
    private val auth: FirebaseAuth
) {

    suspend fun upsertEggProduction(
        flockId: String,
        dateEpochDay: Long,
        totalEggs: Int,
        crackedEggs: Int,
        setForIncubation: Int,
        lineId: String? = null,
        notes: String? = null
    ) {
        db.withTransaction {
            val existing = dao.getByNaturalKey(flockId, lineId, dateEpochDay)
            
            val entity = existing?.copy(
                totalEggs = totalEggs,
                crackedEggs = crackedEggs,
                setForIncubation = setForIncubation,
                notes = notes,
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING,
                deleted = false
            ) ?: EggProductionEntity(
                flockId = flockId,
                lineId = lineId,
                dateEpochDay = dateEpochDay,
                totalEggs = totalEggs,
                crackedEggs = crackedEggs,
                setForIncubation = setForIncubation,
                notes = notes,
                syncState = SyncState.PENDING
            )

            dao.insertOrUpdate(entity)
            
            // 3. Finance Chain: Record Production Cost
            val costResult = unitCostProvider.getEggUnitCost(flockId)
            val unitCost = (costResult as? com.example.hatchtracker.domain.pricing.unitcost.UnitCostResult.Available)?.unitCost ?: 0.0
            val totalCost = unitCost * totalEggs
            
            if (totalCost > 0) {
                val financialEntry = com.example.hatchtracker.data.models.FinancialEntry(
                    ownerId = flockId,
                    ownerType = "flock",
                    type = "cost",
                    category = "production_cost",
                    amount = totalCost,
                    quantity = totalEggs,
                    date = System.currentTimeMillis(),
                    notes = "Automated production cost for $totalEggs eggs on day $dateEpochDay",
                    syncId = "prod_${entity.cloudId}"
                )
                db.financialDao().insertEntry(financialEntry)
            }

            // B2: Log action-based event
            domainEventLogger.log(
                aggregateType = "PRODUCTION_LOG",
                aggregateId = entity.id,
                eventType = "PRODUCTION_UPDATED",
                payloadJson = """{"flockId": "$flockId", "total": $totalEggs, "cracked": $crackedEggs}"""
            )
            
            syncCoordinator.triggerPush()
        }
    }

    suspend fun rebuildSetForIncubationForFlock(flockId: String) {
        db.withTransaction {
            dao.rebuildSetForIncubationForFlock(flockId)
        }
    }

    /**
     * Rebuilds soldEggs cache from active (non-cancelled) sale allocations.
     * Must be called AFTER rebuildSetForIncubationForFlock in DomainEventReplayer.
     */
    suspend fun rebuildSoldEggsForFlock(flockId: String) {
        db.withTransaction {
            dao.rebuildSoldEggsForFlock(flockId)
        }
    }

    suspend fun getEggTotal(flockId: String, fromEpochDay: Long, toEpochDay: Long, lineId: String? = null): Int {
        return dao.getSumTotalEggs(flockId, fromEpochDay, toEpochDay, lineId) ?: 0
    }

    suspend fun getTableEggTotal(flockId: String, fromEpochDay: Long, toEpochDay: Long, lineId: String? = null): Int {
        return dao.getSumTableEggs(flockId, fromEpochDay, toEpochDay, lineId) ?: 0
    }

    suspend fun getAvailableHatchingEggs(flockId: String, daysLookback: Int = 14, lineId: String? = null): Int {
        val today = java.time.LocalDate.now().toEpochDay()
        val fromDay = today - daysLookback
        return dao.getSumTableEggs(flockId, fromDay, today, lineId) ?: 0
    }

    suspend fun reserveEggsForIncubation(flockId: String, incubationId: Long, amount: Int): List<com.example.hatchtracker.data.models.EggReservationEntity> {
        val createdReservations = mutableListOf<com.example.hatchtracker.data.models.EggReservationEntity>()
        val logs = dao.getAvailableLogsForFlock(flockId)
        var remaining = amount
        val now = System.currentTimeMillis()

        for (log in logs) {
            if (remaining <= 0) break
            val availableInLog = log.totalEggs - log.crackedEggs - log.setForIncubation
            if (availableInLog <= 0) continue

            val toTake = minOf(remaining, availableInLog)
            val reservation = com.example.hatchtracker.data.models.EggReservationEntity(
                incubationId = incubationId,
                productionLogId = log.id,
                reservedCount = toTake,
                createdAtEpochMillis = now
            )
            db.eggReservationDao().insertReservation(reservation)
            createdReservations.add(reservation)

            dao.incrementSetForIncubation(log.id, toTake)
            remaining -= toTake
        }

        if (remaining > 0) {
            throw IllegalStateException("Insufficient eggs available for incubation. Still need $remaining eggs.")
        }
        return createdReservations
    }

    suspend fun releaseEggsForIncubation(incubationId: Long): List<com.example.hatchtracker.data.models.EggReservationEntity> {
        val reservations = db.eggReservationDao().getReservationsByIncubation(incubationId)
        for (res in reservations) {
            dao.decrementSetForIncubation(res.productionLogId, res.reservedCount)
        }
        db.eggReservationDao().deleteReservationsByIncubation(incubationId)
        return reservations
    }

    suspend fun softDelete(id: String) {
        db.withTransaction {
            val entity = dao.getById(id) ?: return@withTransaction
            val deleted = entity.copy(
                deleted = true,
                syncState = SyncState.PENDING,
                updatedAt = System.currentTimeMillis()
            )
            dao.insertOrUpdate(deleted)
            
            domainEventLogger.log(
                aggregateType = "PRODUCTION_LOG",
                aggregateId = entity.id,
                eventType = "PRODUCTION_DELETED",
                payloadJson = """{"cloudId": "${entity.cloudId}"}"""
            )
            syncCoordinator.triggerPush()
        }
    }

    fun getBreedLines(flockId: String): kotlinx.coroutines.flow.Flow<List<com.example.hatchtracker.data.models.BreedLineEntity>> {
        return dao.getBreedLines(flockId)
    }

    fun observeFlockProduction(flockId: String, fromEpochDay: Long): kotlinx.coroutines.flow.Flow<List<EggProductionEntity>> {
        return dao.observeRecentProduction(flockId, fromEpochDay)
    }

    suspend fun addBreedLine(flockId: String, label: String, description: String? = null) {
        val entity = com.example.hatchtracker.data.models.BreedLineEntity(
            flockId = flockId,
            label = label,
            syncState = SyncState.PENDING
        )
        dao.insertBreedLine(entity)
        
        domainEventLogger.log(
            aggregateType = "BREED_LINE",
            aggregateId = entity.cloudId,
            eventType = "BREED_LINE_CREATED",
            payloadJson = """{"flockId": "$flockId", "label": "$label"}"""
        )
        syncCoordinator.triggerPush()
    }
}

