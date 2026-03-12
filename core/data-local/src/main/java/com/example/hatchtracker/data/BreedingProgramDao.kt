package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.BreedingProgramEntity
import com.example.hatchtracker.model.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface BreedingProgramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: BreedingProgramEntity)

    @Query("SELECT * FROM breeding_action_plans WHERE syncId = :syncId")
    suspend fun getBySyncId(syncId: String): BreedingProgramEntity?

    @Query("SELECT * FROM breeding_action_plans WHERE scenarioId = :scenarioId AND deleted = 0")
    fun getForScenario(scenarioId: String): Flow<List<BreedingProgramEntity>>

    @Query("SELECT * FROM breeding_action_plans WHERE syncState = 'PENDING'")
    suspend fun getPendingSync(): List<BreedingProgramEntity>

    @Query("UPDATE breeding_action_plans SET syncState = :state, cloudUpdatedAt = :timestamp WHERE syncId = :syncId")
    suspend fun markSynced(syncId: String, state: SyncState, timestamp: Long)
}

