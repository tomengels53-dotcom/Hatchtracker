package com.example.hatchtracker.testing

import com.example.hatchtracker.model.Flock
import com.example.hatchtracker.domain.repo.FlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeFlockRepository : FlockRepository {
    private val flocks = MutableStateFlow<List<Flock>>(emptyList())

    override fun getActiveFlocks(): Flow<List<Flock>> = flocks.asStateFlow()

    override suspend fun getFlockById(id: Long): Flock? {
        return flocks.value.find { it.id == id }
    }

    override suspend fun insertFlock(flock: Flock): Long {
        flocks.update { it + flock }
        return flock.id
    }
    
    fun emit(list: List<Flock>) {
        flocks.value = list
    }
}

