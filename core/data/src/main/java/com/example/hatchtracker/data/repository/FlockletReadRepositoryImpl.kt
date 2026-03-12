package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.domain.breeding.FlockletRef
import com.example.hatchtracker.domain.breeding.FlockletReadRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlockletReadRepositoryImpl @Inject constructor(
    private val db: AppDatabase
) : FlockletReadRepository {
    override suspend fun getFlockletById(id: Long): FlockletRef? {
        val flocklet = db.flockletDao().getFlockletById(id) ?: return null
        return FlockletRef(cloudId = flocklet.syncId)
    }
}
