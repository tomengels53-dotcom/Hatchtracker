package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.core.common.IncubationRegistry
import com.example.hatchtracker.data.models.IncubationProfile

class CatalogRepository(private val db: AppDatabase) {
    private val speciesDao = db.speciesDao()

    /**
     * Seeds the database with default species and breeds if they don't exist.
     * Call this during app startup or first DB access.
     */
    suspend fun seedCatalogIfEmpty() {
        val existingSpecies = speciesDao.getAllSpecies()
        if (existingSpecies.isEmpty()) {
            // Seed Species
            speciesDao.insertSpecies(DataRepository.allSpecies)

            // Seed Breeds
            speciesDao.insertBreeds(DataRepository.allBreeds)
        }
    }

    fun getAllSpeciesFlow() = speciesDao.getAllSpeciesFlow()

    fun getBreedsForSpeciesFlow(speciesId: String) =
        speciesDao.getBreedsForSpeciesFlow(speciesId)

    suspend fun getAllBreeds() = speciesDao.getAllBreeds()

    /**
     * Dynamically resolves the incubation profile for a given breed.
     * Links Breed -> Species -> IncubationProfile.
     */
    suspend fun getIncubationProfileForBreed(breedId: String): IncubationProfile {
        val breed = speciesDao.getBreedById(breedId)
        val species = breed?.speciesId?.replaceFirstChar { it.uppercase() } ?: "Chicken"
        return IncubationRegistry.getProfileForSpecies(species)
    }
}
