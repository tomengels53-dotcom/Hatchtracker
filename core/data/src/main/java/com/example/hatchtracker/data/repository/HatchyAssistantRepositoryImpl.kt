package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.BirdDao
import com.example.hatchtracker.data.FlockDao
import com.example.hatchtracker.data.IncubationDao
import com.example.hatchtracker.domain.hatchy.routing.HatchyAssistantRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HatchyAssistantRepositoryImpl @Inject constructor(
    private val birdDao: BirdDao,
    private val flockDao: FlockDao,
    private val incubationDao: IncubationDao
) : HatchyAssistantRepository {

    override suspend fun getGlobalFlockSummary(): String {
        val totalBirds = birdDao.getAllBirdEntitys().size
        val activeIncubations = incubationDao.getActiveIncubationEntityCount()
        val flocks = flockDao.getAllActiveFlockEntitysOnce()
        
        // Safely extract breeds from active flocks
        val breeds = flocks.flatMap { it.breeds }.distinct().take(5).joinToString(", ")
        
        return """
            📊 Current Flock Summary (Aggregated):
            - Total Birds: $totalBirds
            - Active Incubations: $activeIncubations
            - Number of Active Flocks: ${flocks.size}
            - Main Breeds: ${if (breeds.isEmpty()) "None recorded" else breeds}
        """.trimIndent()
    }

    override suspend fun getBreedingSummary(): String {
        // Placeholder for future expansion; keeps domain logic decoupled for now.
        return "You have several active breeding scenarios. We're currently focusin' on line stability and phenotypic targets."
    }
}
