package com.example.hatchtracker.domain.incubation

import com.example.hatchtracker.domain.subscription.AppCapabilities
import com.example.hatchtracker.model.Incubation
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HatchOutcomeFacade @Inject constructor() {
    val capabilities = MutableStateFlow(AppCapabilities())

    suspend fun getIncubation(id: Long): Incubation? = null

    suspend fun completeIncubation(
        incubationId: Long,
        hatched: Int,
        infertile: Int,
        failed: Int
    ): Result<Unit> = Result.success(Unit)
}
