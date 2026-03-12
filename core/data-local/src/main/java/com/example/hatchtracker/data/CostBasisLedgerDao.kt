package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hatchtracker.data.models.CostBasisLedgerEntryEntity

@Dao
interface CostBasisLedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CostBasisLedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<CostBasisLedgerEntryEntity>)

    @Query("SELECT * FROM cost_basis_ledger WHERE entityId = :entityId")
    suspend fun getLedgerForEntity(entityId: String): List<CostBasisLedgerEntryEntity>

    @Query("SELECT SUM(amount) FROM cost_basis_ledger WHERE entityId = :entityId")
    suspend fun getCostPoolForEntity(entityId: String): Double?
}
