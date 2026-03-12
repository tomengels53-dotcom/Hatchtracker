package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.BreedingDao
import com.example.hatchtracker.data.BirdDao
import com.example.hatchtracker.data.mappers.toEntity
import com.example.hatchtracker.data.models.BreedingRecord
import com.example.hatchtracker.data.models.Bird
import androidx.room.withTransaction

class BreedingRepository @javax.inject.Inject constructor(
    private val breedingDao: BreedingDao,
    private val birdDao: BirdDao
) {
    
    val breedingRecords = breedingDao.getAllBreedingRecordsFlow()

    suspend fun createBreedingRecord(
        sire: Bird,
        dams: List<Bird>,
        flockId: Long,
        species: String,
        goals: List<String>
    ): Long {
        // 1. Create the record
        val record = BreedingRecord(
            flockId = flockId,
            species = species,
            sireId = sire.localId,
            damIds = dams.map { it.localId },
            goals = goals
        )
        val recordId = breedingDao.insertBreedingRecord(record)

        // 2. Lock the birds (Update status to 'breeding')
        val birdsToUpdate = listOf(sire) + dams
        birdsToUpdate.forEach { bird ->
            birdDao.updateBirdEntity(
                bird.copy(status = "breeding", lastUpdated = System.currentTimeMillis()).toEntity()
            )
        }

        return recordId
    }

    fun getAllBreedingRecordsFlow() = breedingDao.getAllBreedingRecordsFlow()

    suspend fun deleteBreedingRecord(record: BreedingRecord) {
        breedingDao.deleteBreedingRecord(record)
    }
}


