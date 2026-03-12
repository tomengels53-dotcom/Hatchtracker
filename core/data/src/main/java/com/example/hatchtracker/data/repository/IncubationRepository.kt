@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.IncubationEntityDao
import com.example.hatchtracker.data.BirdEntityDao
import com.example.hatchtracker.data.DomainEventLogger
import com.example.hatchtracker.data.mappers.toEntity
import com.example.hatchtracker.data.mappers.toModel
import com.example.hatchtracker.model.Incubation
import com.example.hatchtracker.model.SyncState
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.BirdLifecycleStage
import com.example.hatchtracker.model.AuditActionType
import com.example.hatchtracker.data.IncubationMeasurementDao
import com.example.hatchtracker.data.models.IncubationMeasurement
import com.google.firebase.auth.FirebaseAuth
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class IncubationRepository @Inject constructor(
    private val db: AppDatabase,
    private val incubationDao: IncubationEntityDao,
    private val birdDao: BirdEntityDao,
    private val incubationMeasurementDao: IncubationMeasurementDao,
    private val syncCoordinator: com.example.hatchtracker.data.sync.CoreDataSyncCoordinator,
    private val auth: FirebaseAuth,
    private val entitlements: com.example.hatchtracker.billing.Entitlements,
    private val domainEventLogger: DomainEventLogger,
    private val breedRepository: BreedStandardRepository,
    private val breedingAnalyzer: com.example.hatchtracker.core.common.BreedingAnalyzer
) : com.example.hatchtracker.domain.repo.IncubationRepository {
    override val allIncubations: Flow<List<Incubation>> = incubationDao.getAllIncubationEntitysFlow()
    suspend fun insertIncubation(incubation: Incubation): Long {
        return db.withTransaction {
            val activeCount = incubationDao.getActiveIncubationEntityCount()
            if (activeCount >= entitlements.maxActiveIncubations()) {
                throw IllegalStateException("Max active incubations limit reached for current subscription tier.")
            }
            if (incubation.eggsCount > entitlements.maxEggsPerIncubation()) {
                throw IllegalStateException("Egg count exceeds the maximum allowed for current subscription tier.")
            }

            val toInsert = incubation.copy(
                ownerUserId = auth.currentUser?.uid,
                syncState = SyncState.PENDING,
                localUpdatedAt = System.currentTimeMillis()
            ).toEntity().copy(pendingSync = true)
            
            val id = incubationDao.insertIncubationEntity(toInsert)

            domainEventLogger.log(
                aggregateType = "INCUBATION",
                aggregateId = incubation.cloudId,
                eventType = "INCUBATION_INSERTED",
                payloadJson = """{"cloudId": "${incubation.cloudId}", "eggsCount": ${incubation.eggsCount}}"""
            )
            syncCoordinator.triggerPush()
            id
        }
    }

    override suspend fun getActiveIncubationCount(): Int = incubationDao.getActiveIncubationEntityCount()

    suspend fun deleteIncubation(incubation: Incubation) {
        db.withTransaction {
            val toDelete = incubation.copy(
                deleted = true,
                syncState = SyncState.PENDING,
                localUpdatedAt = System.currentTimeMillis()
            ).toEntity().copy(pendingSync = true)
            
            incubationDao.updateIncubationEntity(toDelete)
            domainEventLogger.log(
                aggregateType = "INCUBATION",
                aggregateId = incubation.cloudId,
                eventType = "INCUBATION_SOFT_DELETED",
                payloadJson = """{"cloudId": "${incubation.cloudId}"}"""
            )
            syncCoordinator.triggerPush()
        }
    }

    fun getAllIncubationsFlow(): Flow<List<Incubation>> = 
        incubationDao.getAllIncubationEntitysFlow()
            .map { list -> list.map { it.toModel() } }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()

    suspend fun getAllIncubations(): List<Incubation> = 
        incubationDao.getAllIncubationEntitys().map { it.toModel() }

    suspend fun getRecommendations(): List<com.example.hatchtracker.core.common.Recommendation> {
        val birds = birdDao.getAllBirdEntitys().map { it.toModel() }
        val incubations = incubationDao.getAllIncubationEntitys().map { it.toModel() }
        return com.example.hatchtracker.core.common.BreedingRecommender.getRecommendations(birds, incubations)
    }

    override suspend fun getIncubationById(id: Long): Incubation? = incubationDao.getIncubationEntityById(id)?.toModel()
    suspend fun getIncubationBySyncId(syncId: String) = incubationDao.getIncubationEntityBySyncId(syncId)?.toModel()

    suspend fun getBreedingPerformance(birdId: Long): com.example.hatchtracker.data.models.BreedingPerformance {
        val bird = birdDao.getBirdEntityById(birdId)?.toModel()
        if (bird == null || bird.sex != Sex.FEMALE) {
            return com.example.hatchtracker.data.models.BreedingPerformance(0, 0, 0f, 0f, 0)
        }
        val incubations = incubationDao.getIncubationEntitysByBirdId(birdId).map { it.toModel() }
        val completedIncubations = incubations.filter { it.hatchCompleted }
        if (completedIncubations.isEmpty()) {
            return com.example.hatchtracker.data.models.BreedingPerformance(0, 0, 0f, 0f, 0)
        }
        val totalEggs = completedIncubations.sumOf { it.eggsCount }
        val totalHatched = completedIncubations.sumOf { it.hatchedCount }
        val totalInfertile = completedIncubations.sumOf { it.infertileCount }
        val avgHatchRate = if (totalEggs > 0) (totalHatched.toFloat() / totalEggs) * 100f else 0f
        val infertilityRate = if (totalEggs > 0) (totalInfertile.toFloat() / totalEggs) * 100f else 0f
        val successfulCount = completedIncubations.count { it.hatchedCount > 0 }
        return com.example.hatchtracker.data.models.BreedingPerformance(
            totalEggsSet = totalEggs,
            totalChicksHatched = totalHatched,
            avgHatchRate = avgHatchRate,
            infertilityRate = infertilityRate,
            successfulIncubationsCount = successfulCount
        )
    }

    suspend fun getBirdScore(birdId: Long): Int {
        val bird = birdDao.getBirdEntityById(birdId)?.toModel()
        if (bird == null || bird.sex != Sex.FEMALE) return 0
        val incubations = incubationDao.getIncubationEntitysByBirdId(birdId).map { it.toModel() }
        val children = birdDao.getBirdEntitysByMotherId(birdId).map { it.toModel() }
        return breedingAnalyzer.calculateBreederScore(incubations, children)
    }

    suspend fun update(incubation: Incubation, reason: String? = null) {
        db.withTransaction {
            val current = incubationDao.getIncubationEntityById(incubation.id)
            var updated = incubation
            if (current != null && current.startDate != incubation.startDate) {
                val speciesDays = com.example.hatchtracker.domain.hatchy.knowledge.HatchyPoultryKnowledge.incubationDaysBySpecies[incubation.species] ?: 21
                
                // Breed override logic
                val breedOverride = incubation.breeds.firstOrNull()?.let { breedId ->
                    breedRepository.getBreedById(breedId)?.incubationDurationDays
                }
                
                val incubationDays = breedOverride ?: speciesDays
                val start = java.time.LocalDate.parse(incubation.startDate)
                updated = updated.copy(expectedHatch = start.plusDays(incubationDays.toLong()).toString())
            }
            updated = updated.copy(
                syncState = SyncState.PENDING,
                localUpdatedAt = System.currentTimeMillis()
            )
            
            incubationDao.updateIncubationEntity(updated.toEntity().copy(pendingSync = true))

            domainEventLogger.log(
                aggregateType = "INCUBATION",
                aggregateId = updated.cloudId,
                eventType = "INCUBATION_UPDATED",
                payloadJson = """{"cloudId": "${updated.cloudId}"}"""
            )
            syncCoordinator.triggerPush()

            com.example.hatchtracker.data.audit.AuditLogger.logAction(
                actionType = AuditActionType.UPDATE,
                targetCollection = "incubations",
                targetDocumentId = updated.id.toString(),
                before = current?.let { mapOf("startDate" to it.startDate, "eggsCount" to it.eggsCount, "notes" to it.notes) },
                after = mapOf("startDate" to updated.startDate, "eggsCount" to updated.eggsCount, "notes" to updated.notes),
                reason = reason ?: "Incubation edited by user"
            )
        }
    }

    suspend fun completeHatch(incubationId: Long, hatchedCount: Int, failedCount: Int, notes: String?) {
        val incubation = getIncubationById(incubationId) ?: return
        val updated = incubation.copy(
            hatchedCount = hatchedCount,
            failedCount = failedCount,
            hatchNotes = notes,
            hatchCompleted = true,
            lifecycleStage = BirdLifecycleStage.FLOCKLET,
            lastUpdated = System.currentTimeMillis()
        )
        update(updated)
    }

    suspend fun createIncubationFromActionPlan(
        incubation: Incubation,
        planId: String,
        generationIndex: Int,
        breederPoolBirdIds: List<String>,
        orchestrator: com.example.hatchtracker.domain.breeding.ProgramLifecycleOrchestrator
    ): Long {
        val id = insertIncubation(incubation)
        orchestrator.onIncubationCreated(id, planId, generationIndex, breederPoolBirdIds)
        return id
    }

    suspend fun addMeasurement(measurement: IncubationMeasurement): Long {
        val id = incubationMeasurementDao.insert(measurement)
        val incubation = getIncubationById(measurement.incubationId)
        if (incubation != null) {
            update(incubation.copy(lastUpdated = System.currentTimeMillis()), reason = "Measurement added")
        }
        return id
    }

    fun observeMeasurements(incubationId: Long): Flow<List<IncubationMeasurement>> =
        incubationMeasurementDao.observeForIncubation(incubationId)

    suspend fun getLatestMeasurement(incubationId: Long): IncubationMeasurement? =
        incubationMeasurementDao.getLatest(incubationId)
}
