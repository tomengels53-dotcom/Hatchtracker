package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.BirdEntityDao
import com.example.hatchtracker.data.DomainEventLogger
import com.example.hatchtracker.data.mappers.toEntity
import com.example.hatchtracker.data.mappers.toModel
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.SyncState
import com.google.firebase.auth.FirebaseAuth
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class BirdRepository @Inject constructor(
    private val db: AppDatabase,
    private val birdDao: BirdEntityDao,
    private val syncCoordinator: com.example.hatchtracker.data.sync.CoreDataSyncCoordinator,
    private val auth: FirebaseAuth,
    private val entitlements: com.example.hatchtracker.billing.Entitlements,
    private val domainEventLogger: DomainEventLogger
) : com.example.hatchtracker.domain.repo.BirdRepository {
    override fun getAllBirds(): Flow<List<Bird>> = birdDao.getAllBirdEntitysFlow()
        .map { list -> list.map { it.toModel() } }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        
    @get:JvmName("getAllBirdsFlow")
    val allBirds: Flow<List<Bird>> = getAllBirds()
        
    val activeBirds: Flow<List<Bird>> = birdDao.getActiveBirdEntitysFlow()
        .map { list -> list.map { it.toModel() } }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()

    fun getBirdsByFlockIdFlow(flockId: Long): Flow<List<Bird>> = 
        birdDao.getBirdEntitysByFlockIdFlow(flockId)
            .map { list -> list.map { it.toModel() } }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()

    override suspend fun getBirdById(id: Long): Bird? = birdDao.getBirdEntityById(id)?.toModel()

    fun getBirdByIdFlow(id: Long): Flow<Bird?> = birdDao.getBirdEntityByIdFlow(id)
        .map { it?.toModel() }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()

    suspend fun getAllBirdsSync(): List<Bird> = birdDao.getAllBirdEntitys().map { it.toModel() }

    suspend fun insertBird(bird: Bird): Long {
        return db.withTransaction {
            val flockId = requireNotNull(bird.flockId) { "Bird must have a flockId before insert." }
            val currentCount = birdDao.getBirdEntityCountForFlockSync(flockId)
            if (currentCount >= entitlements.maxBirdsPerFlock()) {
                throw IllegalStateException("Max birds limit reached for this flock in current subscription tier.")
            }

            val toInsert = bird.copy(
                ownerUserId = auth.currentUser?.uid,
                syncState = SyncState.PENDING,
                localUpdatedAt = System.currentTimeMillis()
            ).toEntity().copy(pendingSync = true)
            
            val id = birdDao.insertBirdEntity(toInsert)

            domainEventLogger.log(
                aggregateType = "BIRD",
                aggregateId = bird.cloudId,
                eventType = "BIRD_CREATED",
                payloadJson = """{"flockId": $flockId, "cloudId": "${bird.cloudId}"}"""
            )
            syncCoordinator.triggerPush()
            id
        }
    }

    suspend fun insertBirds(birds: List<Bird>): List<Long> {
        return db.withTransaction {
            if (birds.isEmpty()) return@withTransaction emptyList()

            val flockId = requireNotNull(birds.first().flockId) { "Bird batch insert requires flockId on the first bird." }
            val currentCount = birdDao.getBirdEntityCountForFlockSync(flockId)
            if (currentCount + birds.size > entitlements.maxBirdsPerFlock()) {
                throw IllegalStateException("Batch operation would exceed max birds limit for this flock.")
            }

            val userId = auth.currentUser?.uid
            val now = System.currentTimeMillis()
            val entitiesToInsert = birds.map { 
                it.copy(ownerUserId = userId, syncState = SyncState.PENDING, localUpdatedAt = now)
                  .toEntity()
                  .copy(pendingSync = true)
            }
            val ids = birdDao.insertBirdEntitys(entitiesToInsert)

            birds.forEach { bird ->
                domainEventLogger.log(
                    aggregateType = "BIRD",
                    aggregateId = bird.cloudId,
                    eventType = "BIRD_CREATED",
                    payloadJson = """{"flockId": $flockId, "cloudId": "${bird.cloudId}"}"""
                )
            }
            syncCoordinator.triggerPush()
            ids
        }
    }

    suspend fun updateBird(bird: Bird) {
        db.withTransaction {
            val toUpdate = bird.copy(
                syncState = SyncState.PENDING,
                localUpdatedAt = System.currentTimeMillis()
            ).toEntity().copy(pendingSync = true)
            
            birdDao.updateBirdEntity(toUpdate)
            domainEventLogger.log(
                aggregateType = "BIRD",
                aggregateId = bird.cloudId,
                eventType = "BIRD_UPDATED",
                payloadJson = """{"cloudId": "${bird.cloudId}"}"""
            )
            syncCoordinator.triggerPush()
        }
    }

    suspend fun deleteBird(bird: Bird) {
        db.withTransaction {
            val toDelete = bird.copy(
                deleted = true,
                syncState = SyncState.PENDING,
                localUpdatedAt = System.currentTimeMillis()
            ).toEntity().copy(pendingSync = true)
            
            birdDao.updateBirdEntity(toDelete)
            domainEventLogger.log(
                aggregateType = "BIRD",
                aggregateId = bird.cloudId,
                eventType = "BIRD_DELETED",
                payloadJson = """{"cloudId": "${bird.cloudId}"}"""
            )
            syncCoordinator.triggerPush()
        }
    }

    suspend fun getBirdsByMotherId(motherId: Long): List<Bird> = 
        birdDao.getBirdEntitysByMotherId(motherId).map { it.toModel() }
}
