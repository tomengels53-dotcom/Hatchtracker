package com.example.hatchtracker.data.repository

import androidx.room.withTransaction
import com.example.hatchtracker.core.domain.models.AssetAllocationEvent
import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.AssetAllocationDao
import com.example.hatchtracker.data.DomainEventLogger
import com.example.hatchtracker.data.models.AssetAllocationEventEntity
import com.example.hatchtracker.data.sync.CoreDataSyncCoordinator
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class AssetAllocationRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: AssetAllocationDao,
    private val syncCoordinator: CoreDataSyncCoordinator,
    private val auth: FirebaseAuth,
    private val domainEventLogger: DomainEventLogger
) {

    suspend fun getAllocation(allocationId: String): AssetAllocationEvent? {
        return dao.getAllocation(allocationId)?.toDomain()
    }

    suspend fun addAllocation(event: AssetAllocationEvent) {
        val userId = auth.currentUser?.uid ?: return
        val entity = event.toEntity(userId)
        
        db.withTransaction {
            dao.insertAllocation(entity)
            domainEventLogger.log(
                aggregateType = "ASSET_ALLOCATION",
                aggregateId = entity.allocationId,
                eventType = "ASSET_ALLOCATED",
                payloadJson = """{"allocationId": "${entity.allocationId}", "assetId": "${entity.assetId}"}"""
            )
            syncCoordinator.triggerPush()
        }
    }
    
    suspend fun getAllocationsForAsset(assetId: String): List<AssetAllocationEvent> {
        return dao.getAllocationsForAsset(assetId).map { it.toDomain() }
    }

    private fun AssetAllocationEventEntity.toDomain() = AssetAllocationEvent(
        allocationId = allocationId,
        assetId = assetId,
        scopeType = scopeType,
        scopeId = scopeId,
        periodKey = periodKey,
        amount = amount,
        createdAt = createdAt
    )

    private fun AssetAllocationEvent.toEntity(userId: String) = AssetAllocationEventEntity(
        allocationId = allocationId,
        assetId = assetId,
        scopeType = scopeType,
        scopeId = scopeId,
        periodKey = periodKey,
        amount = amount,
        syncStateInt = 1, // PENDING
        ownerUserId = userId,
        createdAt = createdAt
    )
}
