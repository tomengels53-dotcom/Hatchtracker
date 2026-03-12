package com.example.hatchtracker.domain.repo

import com.example.hatchtracker.model.Bird
import kotlinx.coroutines.flow.Flow

interface BirdRepository {
    fun getAllBirds(): Flow<List<Bird>>
    suspend fun getBirdById(id: Long): Bird?
}
