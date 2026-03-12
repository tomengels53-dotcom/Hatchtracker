package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.BreedingProgramDao
import com.example.hatchtracker.data.models.BreedingProgram
import com.example.hatchtracker.data.DomainEventLogger
import com.example.hatchtracker.model.SyncState
import com.example.hatchtracker.data.models.toDomainModel
import com.example.hatchtracker.data.models.toEntity
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BreedingProgramRepository @Inject constructor(
    private val db: AppDatabase,
    private val planDao: BreedingProgramDao,
    private val syncCoordinator: com.example.hatchtracker.data.sync.CoreDataSyncCoordinator,
    private val domainEventLogger: com.example.hatchtracker.data.DomainEventLogger
) {
    /**
     * Observes active implementation plans for a user.
     */
    fun observePlans(userId: String): Flow<List<BreedingProgram>> {
        // userId restriction should be handled by the DAO or by filtering if syncId isn't global
        return planDao.getForScenario("").map { list -> 
             list.map { it.toDomainModel() }
        }
    }

    /**
     * Creates a new action plan.
     */
    suspend fun createPlan(plan: BreedingProgram): Result<String> {
        return try {
            val syncId = if (plan.id.isEmpty()) UUID.randomUUID().toString() else plan.id
            val toInsert = plan.copy(id = syncId).toEntity().copy(
                syncState = SyncState.PENDING,
                lastModified = System.currentTimeMillis()
            )

            db.withTransaction {
                planDao.insert(toInsert)
                domainEventLogger.log(
                    aggregateType = "BREEDING_PROGRAM",
                    aggregateId = toInsert.syncId,
                    eventType = "BREEDING_PROGRAM_CREATED",
                    payloadJson = """{"syncId": "$syncId"}"""
                )
            }
            syncCoordinator.triggerPush()
            Result.success(syncId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing plan.
     */
    suspend fun updatePlan(plan: BreedingProgram): Result<Unit> {
        return try {
            if (plan.id.isEmpty()) throw Exception("Plan ID is required for update")
            
            val toUpdate = plan.toEntity().copy(
                syncState = SyncState.PENDING,
                lastModified = System.currentTimeMillis()
            )

            db.withTransaction {
                planDao.insert(toUpdate)
                domainEventLogger.log(
                    aggregateType = "BREEDING_PROGRAM",
                    aggregateId = toUpdate.syncId,
                    eventType = "BREEDING_PROGRAM_UPDATED",
                    payloadJson = """{"syncId": "${plan.id}"}"""
                )
            }
            syncCoordinator.triggerPush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a plan (Soft Delete).
     */
    suspend fun deletePlan(planId: String): Result<Unit> {
        return try {
            val existing = planDao.getBySyncId(planId) 
                ?: return Result.failure(Exception("Plan not found"))
            
            val toDelete = existing.copy(
                deleted = true,
                syncState = SyncState.PENDING,
                lastModified = System.currentTimeMillis()
            )

            db.withTransaction {
                planDao.insert(toDelete)
                domainEventLogger.log(
                    aggregateType = "BREEDING_PROGRAM",
                    aggregateId = toDelete.syncId,
                    eventType = "BREEDING_PROGRAM_DELETED",
                    payloadJson = """{"syncId": "$planId"}"""
                )
            }
            syncCoordinator.triggerPush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPlan(planId: String): Result<BreedingProgram> {
        return try {
            val entity = planDao.getBySyncId(planId)
            if (entity != null) {
                Result.success(entity.toDomainModel())
            } else {
                Result.failure(Exception("Plan not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

