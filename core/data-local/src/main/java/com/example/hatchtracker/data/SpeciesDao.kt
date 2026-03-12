package com.example.hatchtracker.data

import androidx.room.*
import com.example.hatchtracker.data.models.Breed
import com.example.hatchtracker.data.models.Species
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeciesDao {
    // Species Queries
    @Query("SELECT * FROM species_catalog ORDER BY name ASC")
    fun getAllSpeciesFlow(): Flow<List<Species>>

    @Query("SELECT * FROM species_catalog")
    suspend fun getAllSpecies(): List<Species>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecies(species: List<Species>): List<Long>

    // Breed Queries
    @Query("SELECT * FROM breed_catalog WHERE speciesId = :speciesId ORDER BY name ASC")
    fun getBreedsForSpeciesFlow(speciesId: String): Flow<List<Breed>>

    @Query("SELECT * FROM breed_catalog")
    suspend fun getAllBreeds(): List<Breed>

    @Query("SELECT * FROM breed_catalog WHERE id = :id")
    suspend fun getBreedById(id: String): Breed?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreeds(breeds: List<Breed>): List<Long>

    @Update
    suspend fun updateBreed(breed: Breed): Int

    @Delete
    suspend fun deleteBreed(breed: Breed): Int

    // Sync Helper
    @Query("SELECT * FROM breed_catalog WHERE lastUpdated > :timestamp")
    suspend fun getBreedsUpdatedAfter(timestamp: Long): List<Breed>
}
