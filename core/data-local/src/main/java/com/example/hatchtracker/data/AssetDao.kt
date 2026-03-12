package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.hatchtracker.data.models.AssetEntity
import com.example.hatchtracker.core.domain.models.AssetStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets WHERE ownerUserId = :userId AND deleted = 0 ORDER BY purchaseDateEpochMs DESC")
    fun observeAssets(userId: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE assetId = :id")
    suspend fun getAssetById(id: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE ownerUserId = :userId AND status = :status AND deleted = 0 ORDER BY purchaseDateEpochMs DESC")
    suspend fun getAssetsByStatusForUser(userId: String, status: AssetStatus): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE status = :status AND deleted = 0")
    suspend fun getAssetsByStatus(status: AssetStatus): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE linkedDeviceId = :deviceId AND status = :status AND deleted = 0 ORDER BY purchaseDateEpochMs DESC LIMIT 1")
    suspend fun getActiveAssetByDeviceId(deviceId: String, status: AssetStatus = AssetStatus.ACTIVE): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Update
    suspend fun updateAsset(asset: AssetEntity)
}
