package com.example.hatchtracker.domain.repo

import com.example.hatchtracker.model.Incubation
import kotlinx.coroutines.flow.Flow

interface IncubationRepository {
    val allIncubations: Flow<List<Incubation>>
    suspend fun getActiveIncubationCount(): Int
    suspend fun getIncubationById(id: Long): Incubation?
}
