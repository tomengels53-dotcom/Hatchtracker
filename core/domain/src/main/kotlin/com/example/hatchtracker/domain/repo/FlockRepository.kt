package com.example.hatchtracker.domain.repo

import com.example.hatchtracker.model.Flock
import kotlinx.coroutines.flow.Flow

interface FlockRepository {
    val allActiveFlocks: Flow<List<Flock>>
    suspend fun getFlockById(id: Long): Flock?
    fun getFlockFlow(id: Long): Flow<Flock?>
    suspend fun insertFlock(flock: Flock): Long
    suspend fun updateFlock(flock: Flock)
    suspend fun deleteFlock(flock: Flock)
    fun getBirdCountForFlock(flockId: Long): Flow<Int>
    suspend fun refreshFlockBreeds(flockId: Long)
}

