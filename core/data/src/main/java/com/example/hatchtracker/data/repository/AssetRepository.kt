package com.example.hatchtracker.data.repository

import androidx.room.withTransaction
import com.example.hatchtracker.core.domain.models.Asset
import com.example.hatchtracker.core.domain.models.AssetStatus
import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.AssetDao
import com.example.hatchtracker.data.models.AssetEntity
import com.example.hatchtracker.data.DomainEventLogger
import com.example.hatchtracker.data.sync.CoreDataSyncCoordinator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AssetRepository @Inject constructor(
    private val db: AppDatabase,
    private val assetDao: AssetDao,
    private val syncCoordinator: CoreDataSyncCoordinator,
    private val auth: FirebaseAuth,
    private val domainEventLogger: com.example.hatchtracker.data.DomainEventLogger
) {
    fun observeAssets(): Flow<List<Asset>> {
        val userId = auth.currentUser?.uid ?: return flowOf(emptyList())
        return assetDao.observeAssets(userId).map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun getAsset(assetId: String): Asset? {
        return assetDao.getAssetById(assetId)?.toDomain()
    }

    suspend fun getActiveAssetByDeviceId(deviceId: String): Asset? {
        return assetDao.getActiveAssetByDeviceId(deviceId)?.toDomain()
    }

    suspend fun getActiveAssets(): List<Asset> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return assetDao.getAssetsByStatusForUser(userId, AssetStatus.ACTIVE).map { it.toDomain() }
    }

    suspend fun addAsset(asset: Asset): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val entity = asset.toEntity(userId)
        
        db.withTransaction {
            assetDao.insertAsset(entity)
            domainEventLogger.log(
                aggregateType = "ASSET",
                aggregateId = entity.assetId,
                eventType = "ASSET_CREATED",
                payloadJson = """{"assetId": "${entity.assetId}"}"""
            )
            syncCoordinator.triggerPush()
        }
        return entity.assetId
    }

    suspend fun updateAsset(asset: Asset) {
        val userId = auth.currentUser?.uid ?: return
        val entity = asset.toEntity(userId)
        db.withTransaction {
            assetDao.updateAsset(entity)
            domainEventLogger.log(
                aggregateType = "ASSET",
                aggregateId = entity.assetId,
                eventType = "ASSET_UPDATED",
                payloadJson = """{"assetId": "${entity.assetId}"}"""
            )
            syncCoordinator.triggerPush()
        }
    }

    private fun AssetEntity.toDomain() = Asset(
        assetId = assetId,
        name = name,
        category = category,
        linkedDeviceId = linkedDeviceId,
        purchaseDateEpochMs = purchaseDateEpochMs,
        purchasePrice = purchasePrice,
        residualValue = residualValue,
        depreciationMethod = depreciationMethod,
        usefulLifeMonths = usefulLifeMonths,
        expectedCycles = expectedCycles,
        cyclesAllocatedCount = cyclesAllocatedCount,
        lastAllocatedAtEpochMs = lastAllocatedAtEpochMs,
        retiredDateEpochMs = retiredDateEpochMs,
        retirementValue = retirementValue,
        status = status
    )

    private fun Asset.toEntity(userId: String) = AssetEntity(
        assetId = assetId,
        name = name,
        category = category,
        linkedDeviceId = linkedDeviceId,
        purchaseDateEpochMs = purchaseDateEpochMs,
        purchasePrice = purchasePrice,
        residualValue = residualValue,
        depreciationMethod = depreciationMethod,
        usefulLifeMonths = usefulLifeMonths,
        expectedCycles = expectedCycles,
        cyclesAllocatedCount = cyclesAllocatedCount,
        lastAllocatedAtEpochMs = lastAllocatedAtEpochMs,
        retiredDateEpochMs = retiredDateEpochMs,
        retirementValue = retirementValue,
        status = status,
        ownerUserId = userId,
        syncStateInt = 1, // PENDING
        updatedAt = System.currentTimeMillis()
    )
}
