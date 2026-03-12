package com.example.hatchtracker.domain.repo

import com.example.hatchtracker.model.Flocklet
import kotlinx.coroutines.flow.Flow

interface NurseryRepository {
    val activeFlocklets: Flow<List<Flocklet>>
    suspend fun getFlockletsForHatch(hatchId: Long): Flow<List<Flocklet>>
    suspend fun getFlockletById(id: Long): Flocklet?
}
