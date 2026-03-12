package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingProgram
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Incubation
import kotlinx.coroutines.flow.Flow

interface BreedingProgramRepository {
    fun observePlans(userId: String): Flow<List<BreedingProgram>>
    suspend fun getPlan(planId: String): Result<BreedingProgram>
    suspend fun updatePlan(plan: BreedingProgram): Result<Unit>
}

interface BirdRepository {
    val allBirds: Flow<List<Bird>>
}

interface IncubationRepository {
    suspend fun getIncubationById(incubationId: Long): Incubation?
    suspend fun update(incubation: Incubation, reason: String)
}

