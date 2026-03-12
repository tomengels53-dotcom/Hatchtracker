@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.models.Flocklet
import com.example.hatchtracker.core.common.NurseryConfig
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.mappers.toEntity
import com.example.hatchtracker.data.mappers.toModel
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NurseryRepository @Inject constructor(
    private val database: AppDatabase,
    private val graduationListener: com.example.hatchtracker.domain.breeding.FlockletGraduationListener,
    private val flockRepository: FlockRepository,
    private val entitlements: com.example.hatchtracker.billing.Entitlements,
    private val domainEventLogger: com.example.hatchtracker.data.DomainEventLogger,
    private val breedRepository: BreedStandardRepository
) : com.example.hatchtracker.domain.repo.NurseryRepository {
    
    private val flockletDao = database.flockletDao()

    override val activeFlocklets: Flow<List<Flocklet>> =
        flockletDao.getActiveFlockletsFlow().map { list -> list.map { it.toModel() } }

    override suspend fun getFlockletsForHatch(hatchId: Long): Flow<List<Flocklet>> =
        flockletDao.getFlockletsByHatchId(hatchId).map { list -> list.map { it.toModel() } }

    suspend fun getFlockletByHatchId(hatchId: Long) = flockletDao.getFlockletByHatchId(hatchId)?.toModel()

    suspend fun getFlockletsByHatchIdSync(hatchId: Long) = flockletDao.getFlockletsByHatchIdSync(hatchId).map { it.toModel() }

    override suspend fun getFlockletById(id: Long): Flocklet? = flockletDao.getFlockletById(id)?.toModel()

    suspend fun addFlocklet(flocklet: Flocklet): Flocklet {
        // Enforcement: Check Max Flocklets
        val activeCount = flockletDao.getActiveFlockletsSync().size
        if (activeCount >= entitlements.maxFlocklets()) {
            throw IllegalStateException("Max active flocklets limit reached for current subscription tier.")
        }

        // Initialize targets if fresh
        val rule = NurseryConfig.getRuleForSpecies(flocklet.species)
        val initialized = flocklet.copy(
            targetTemp = rule.initialTemp,
            currentTemp = if (flocklet.currentTemp == 0.0) rule.initialTemp else flocklet.currentTemp
        )
        val newId = flockletDao.insertFlocklet(initialized.toEntity())
        
        domainEventLogger.log(
            aggregateType = "FLOCKLET",
            aggregateId = newId.toString(),
            eventType = "FLOCKLET_CREATED",
            payloadJson = """{"species": "${initialized.species}", "chickCount": ${initialized.chickCount}, "hatchId": ${initialized.hatchId}}"""
        )

        return initialized.copy(id = newId)
    }

    suspend fun ensureFlockletForHatch(
        incubation: Incubation,
        hatchedCount: Int,
        hatchDate: LocalDate = LocalDate.now(),
        fallbackBreed: String? = null,
        costBasisCents: Long = 0,
        costBasisSourceRef: String? = null
    ): Flocklet? {
        if (hatchedCount <= 0) return null

        val existing = incubation.id.let { flockletDao.getFlockletByHatchId(it) }
        if (existing != null) return existing.toModel()

        // Enforcement: Check Max Chicks
        if (hatchedCount > entitlements.maxChicksPerFlocklet()) {
            // Note: We don't throw here to avoid breaking the completed hatch flow, 
            // but we could cap it or log a warning. For now, following the "Hard Guard" 
            // strategy for creation we will throw to ensure compliance.
            throw IllegalStateException("Chick count exceeds the maximum allowed for current subscription tier.")
        }

        val flocklet = Flocklet(
            hatchId = incubation.id,
            species = incubation.species,
            breeds = incubation.breeds.ifEmpty { listOf(fallbackBreed ?: "Mixed") },
            hatchDate = hatchDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            chickCount = hatchedCount,
            currentTemp = 0.0, // Initialized dynamically in addFlocklet
            targetTemp = 0.0,
            notes = "Automatically created from incubation hatch.",
            costBasisCents = costBasisCents,
            costBasisSourceRef = costBasisSourceRef
        )

        return addFlocklet(flocklet)
    }

    suspend fun updateFlocklet(flocklet: Flocklet) {
        // Recalculate readiness
        val rule = NurseryConfig.getRuleForSpecies(flocklet.species)
        val isReady = flocklet.ageInDays >= rule.minAgeForFlock

        val updated = flocklet.copy(
            readyForFlock = isReady,
            lastUpdated = System.currentTimeMillis()
        )
        flockletDao.updateFlocklet(updated.toEntity())
    }

    suspend fun graduateFlocklet(flockletId: Long, targetFlockId: Long?) {
        val flocklet = flockletDao.getFlockletById(flockletId)?.toModel() ?: return
        val updated = flocklet.copy(
            readyForFlock = true,
            movedToFlockId = targetFlockId ?: -1, // -1 if just archived/graduated without tracking destination yet
            lastUpdated = System.currentTimeMillis()
        )
        flockletDao.updateFlocklet(updated.toEntity())

        domainEventLogger.log(
            aggregateType = "FLOCKLET",
            aggregateId = flockletId.toString(),
            eventType = "FLOCKLET_GRADUATED",
            payloadJson = """{"targetFlockId": $targetFlockId}"""
        )
    }

    suspend fun deleteFlocklet(flocklet: Flocklet) {
        val id = flocklet.id
        flockletDao.deleteFlocklet(flocklet.toEntity())
        
        domainEventLogger.log(
            aggregateType = "FLOCKLET",
            aggregateId = id.toString(),
            eventType = "FLOCKLET_DELETED",
            payloadJson = """{"reason": "Manual deletion"}"""
        )
    }

    suspend fun performDailyUpdates() {
         val flocklets = flockletDao.getActiveFlockletsSync().map { it.toModel() }
         flocklets.forEach { flocklet ->
             val updated = calculateDailyState(flocklet)
             if (updated.ageInDays != flocklet.ageInDays || updated.targetTemp != flocklet.targetTemp) {
                 updateFlocklet(updated)
             }
         }
    }
    
    fun calculateDailyState(flocklet: Flocklet): Flocklet {
        val rule = NurseryConfig.getRuleForSpecies(flocklet.species)
        
        val now = System.currentTimeMillis()
        val daysOld = TimeUnit.MILLISECONDS.toDays(now - flocklet.hatchDate).toInt()
        
        var newTarget = rule.initialTemp - (daysOld * rule.tempReductionPerDay)
        if (newTarget < rule.minSurvivalTemp) newTarget = rule.minSurvivalTemp
        
        // Breed override logic
        val breedOverride = flocklet.breeds.firstOrNull()?.let { breedId ->
            breedRepository.getBreedById(breedId)?.nurseryGraduationDays
        }
        
        val minAge = breedOverride ?: rule.minAgeForFlock
        val isReady = daysOld >= minAge

        return flocklet.copy(
            ageInDays = daysOld,
            targetTemp = newTarget,
            readyForFlock = isReady
        )
    }

    /**
     * Transitions a flocklet from the nursery to the adult flock.
     */
    suspend fun graduateToAdult(flockletId: Long, targetFlockId: Long) {
        val flocklet = flockletDao.getFlockletById(flockletId)?.toModel() ?: return
        
        // Phase 3: Transfer Frozen Costs
        val perBirdCostCents = if (flocklet.chickCount > 0) flocklet.costBasisCents / flocklet.chickCount else 0L
        
        database.withTransaction {
            val updated = flocklet.copy(
                lifecycleStage = com.example.hatchtracker.model.BirdLifecycleStage.ADULT,
                movedToFlockId = targetFlockId,
                readyForFlock = true,
                lastUpdated = System.currentTimeMillis()
            )
            flockletDao.updateFlocklet(updated.toEntity())
            
            // Update individual birds associated with this hatch
            flocklet.hatchId?.let { hatchId ->
                val birds = database.birdDao().getBirdEntitysByIncubationId(hatchId)
                birds.forEach { bird ->
                    database.birdDao().updateBirdEntity(bird.copy(
                        flockId = targetFlockId,
                        lifecycleStage = com.example.hatchtracker.model.BirdLifecycleStage.ADULT,
                        costBasisCents = perBirdCostCents,
                        costBasisSourceRef = flocklet.costBasisSourceRef ?: "flocklet_${flocklet.syncId}",
                        lastUpdated = System.currentTimeMillis()
                    ))
                }
            }
            
            // Notify Action Plans
            graduationListener.onFlockletGraduated(flockletId, targetFlockId)
            
            // Refresh breeds in target flock
            flockRepository.refreshFlockBreeds(targetFlockId)
        }
    }

    suspend fun recordFlockletLoss(flockletId: Long, count: Int, reason: String): Result<Unit> {
        val flocklet = flockletDao.getFlockletById(flockletId)?.toModel()
            ?: return Result.failure(Exception("Flocklet not found"))

        if (count > flocklet.chickCount) {
             return Result.failure(Exception("Cannot record loss greater than current count"))
        }

        val newCount = flocklet.chickCount - count
        val noteEntry = "[${java.time.LocalDate.now()}] Loss: -$count. Reason: $reason"
        val newNotes = if (flocklet.notes.isNullOrBlank()) noteEntry else "${flocklet.notes}\n$noteEntry"

        return try {
            if (newCount == 0) {
                // If all are lost, delete the flocklet
                flockletDao.deleteFlocklet(flocklet.toEntity())
                domainEventLogger.log(
                    aggregateType = "FLOCKLET",
                    aggregateId = flocklet.id.toString(),
                    eventType = "FLOCKLET_DELETED",
                    payloadJson = """{"reason": "Total loss: $reason"}"""
                )
            } else {
                val updated = flocklet.copy(
                    chickCount = newCount,
                    notes = newNotes,
                    lastUpdated = System.currentTimeMillis()
                )
                flockletDao.updateFlocklet(updated.toEntity())
                domainEventLogger.log(
                    aggregateType = "FLOCKLET",
                    aggregateId = flocklet.id.toString(),
                    eventType = "FLOCKLET_LOSS_RECORDED",
                    payloadJson = """{"lossCount": $count, "reason": "$reason", "remaining": $newCount}"""
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

