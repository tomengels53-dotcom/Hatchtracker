package com.example.hatchtracker.data.repository

import androidx.room.withTransaction
import com.example.hatchtracker.core.domain.models.CostBasisLedgerEntry
import com.example.hatchtracker.core.domain.models.LedgerEntityType
import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.CostBasisLedgerDao
import com.example.hatchtracker.data.DomainEventLogger
import com.example.hatchtracker.data.models.CostBasisLedgerEntryEntity
import com.example.hatchtracker.data.sync.CoreDataSyncCoordinator
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class CostBasisRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: CostBasisLedgerDao,
    private val syncCoordinator: CoreDataSyncCoordinator,
    private val auth: FirebaseAuth,
    private val domainEventLogger: DomainEventLogger
) {

    suspend fun getCostBasisTotal(entityType: LedgerEntityType, entityId: String): Double {
        return dao.getCostPoolForEntity(entityId) ?: 0.0
    }

    suspend fun addLedgerEntry(entry: CostBasisLedgerEntry) {
        val userId = auth.currentUser?.uid ?: return
        val entity = entry.toEntity(userId)
        
        db.withTransaction {
            dao.insertEntry(entity)
            domainEventLogger.log(
                aggregateType = "COST_BASIS",
                aggregateId = entity.entryId,
                eventType = "COST_BASIS_ENTRY_ADDED",
                payloadJson = """{"entryId": "${entity.entryId}", "amount": ${entity.amount}}"""
            )
            syncCoordinator.triggerPush()
        }
    }

    suspend fun addLedgerEntries(entries: List<CostBasisLedgerEntry>) {
        val userId = auth.currentUser?.uid ?: return
        val entities = entries.map { it.toEntity(userId) }
        
        db.withTransaction {
            dao.insertEntries(entities)
            entities.forEach { entity ->
                domainEventLogger.log(
                    aggregateType = "COST_BASIS",
                    aggregateId = entity.entryId,
                    eventType = "COST_BASIS_ENTRY_ADDED",
                    payloadJson = """{"entryId": "${entity.entryId}", "amount": ${entity.amount}}"""
                )
            }
            syncCoordinator.triggerPush()
        }
    }

    private fun CostBasisLedgerEntryEntity.toDomain() = CostBasisLedgerEntry(
        entryId = entryId,
        entityType = entityType,
        entityId = entityId,
        sourceType = sourceType,
        amount = amount,
        createdAt = createdAt
    )

    private fun CostBasisLedgerEntry.toEntity(userId: String) = CostBasisLedgerEntryEntity(
        entryId = entryId,
        entityType = entityType,
        entityId = entityId,
        sourceType = sourceType,
        amount = amount,
        ownerUserId = userId,
        syncStateInt = 1, // PENDING
        createdAt = createdAt
    )
}
