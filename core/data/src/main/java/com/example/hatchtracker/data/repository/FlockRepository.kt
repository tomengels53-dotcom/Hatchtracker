
package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.FlockEntityDao
import com.example.hatchtracker.data.DomainEventLogger
import com.example.hatchtracker.data.mappers.toEntity
import com.example.hatchtracker.data.mappers.toModel
import com.example.hatchtracker.model.Flock
import com.example.hatchtracker.model.SyncState
import com.example.hatchtracker.model.AuditActionType
import com.google.firebase.auth.FirebaseAuth
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class FlockRepository @Inject constructor(
    private val db: AppDatabase,
    private val flockDao: FlockEntityDao,
    private val syncCoordinator: com.example.hatchtracker.data.sync.CoreDataSyncCoordinator,
    private val auth: FirebaseAuth,
    private val entitlements: com.example.hatchtracker.billing.Entitlements,
    private val domainEventLogger: DomainEventLogger
) {
    val allActiveFlocks: Flow<List<Flock>> = flockDao.getAllActiveFlockEntitys()
        .map { list -> list.map { it.toModel() } }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        
    val activeFlocks: Flow<List<Flock>> = allActiveFlocks

    suspend fun getFlockById(id: Long): Flock? = flockDao.getFlockEntityById(id)?.toModel()

    fun getFlockFlow(id: Long): Flow<Flock?> = flockDao.getFlockEntityFlow(id)
        .map { it?.toModel() }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()

    suspend fun insertFlock(flock: Flock): Long {
        return db.withTransaction {
            val currentCount = flockDao.getActiveFlockEntityCountSync()
            if (currentCount >= entitlements.maxFlocks()) {
                throw IllegalStateException("Max flocks limit reached for current subscription tier.")
            }

            val toInsert = flock.copy(
                ownerUserId = auth.currentUser?.uid,
                syncState = SyncState.PENDING,
                localUpdatedAt = System.currentTimeMillis()
            ).toEntity().copy(pendingSync = true)
            
            val id = flockDao.insertFlockEntity(toInsert)

            domainEventLogger.log(
                aggregateType = "FLOCK",
                aggregateId = flock.cloudId,
                eventType = "FLOCK_CREATED",
                payloadJson = """{"cloudId": "${flock.cloudId}", "name": "${flock.name}"}"""
            )
            syncCoordinator.triggerPush()
            id
        }
    }

    suspend fun updateFlock(flock: Flock, reason: String? = null) {
        db.withTransaction {
            val current = flockDao.getFlockEntityById(flock.localId)
            val toUpdate = flock.copy(
                syncState = SyncState.PENDING,
                localUpdatedAt = System.currentTimeMillis()
            ).toEntity().copy(pendingSync = true)
            
            flockDao.updateFlockEntity(toUpdate)

            domainEventLogger.log(
                aggregateType = "FLOCK",
                aggregateId = flock.cloudId,
                eventType = "FLOCK_UPDATED",
                payloadJson = """{"cloudId": "${flock.cloudId}"}"""
            )
            syncCoordinator.triggerPush()

            com.example.hatchtracker.data.audit.AuditLogger.logAction(
                actionType = AuditActionType.UPDATE,
                targetCollection = "flocks",
                targetDocumentId = flock.localId.toString(),
                before = current?.let { mapOf("name" to it.name, "purpose" to it.purpose, "active" to it.active, "notes" to it.notes) },
                after = mapOf("name" to flock.name, "purpose" to flock.purpose, "active" to flock.active, "notes" to flock.notes),
                reason = reason ?: "Flock edited by user"
            )
        }
    }

    suspend fun deleteFlock(flock: Flock) {
        db.withTransaction {
            val toDelete = flock.copy(
                active = false,
                deleted = true,
                syncState = SyncState.PENDING,
                localUpdatedAt = System.currentTimeMillis()
            ).toEntity().copy(pendingSync = true)
            
            flockDao.updateFlockEntity(toDelete)

            domainEventLogger.log(
                aggregateType = "FLOCK",
                aggregateId = flock.cloudId,
                eventType = "FLOCK_DELETED",
                payloadJson = """{"cloudId": "${flock.cloudId}"}"""
            )
            syncCoordinator.triggerPush()
        }
    }

    fun getBirdCountForFlock(flockId: Long): Flow<Int> = flockDao.getBirdCountForFlockEntity(flockId)
        .distinctUntilChanged()

    fun getFlocksBySpecies(species: String): Flow<List<Flock>> =
        allActiveFlocks
            .map { flocks -> flocks.filter { it.species.name.equals(species, ignoreCase = true) } }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()

    suspend fun refreshFlockBreeds(flockId: Long) {
        val distinctBreeds = flockDao.getDistinctBreedsForFlockEntity(flockId)
        val flock = flockDao.getFlockEntityById(flockId)
        if (flock != null) {
            val updatedFlock = flock.copy(breeds = distinctBreeds.ifEmpty { listOf("Mixed") })
            if (flock.breeds != updatedFlock.breeds) {
                flockDao.updateFlockEntity(updatedFlock)
            }
        }
    }
}
