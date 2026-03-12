package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hatchtracker.data.models.AssetAllocationEventEntity

@Dao
interface AssetAllocationDao {
    @Query("SELECT * FROM asset_allocation_events WHERE allocationId = :allocationId")
    suspend fun getAllocation(allocationId: String): AssetAllocationEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: AssetAllocationEventEntity)
    
    @Query("SELECT * FROM asset_allocation_events WHERE assetId = :assetId")
    suspend fun getAllocationsForAsset(assetId: String): List<AssetAllocationEventEntity>
}
